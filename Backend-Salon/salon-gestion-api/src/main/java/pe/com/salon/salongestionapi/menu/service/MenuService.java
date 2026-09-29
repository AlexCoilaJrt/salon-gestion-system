package pe.com.salon.salongestionapi.menu.service;

import pe.com.salon.salongestionapi.menu.dto.MenuItemDTO;
import pe.com.salon.salongestionapi.menu.entity.MenuItem;
import pe.com.salon.salongestionapi.menu.repository.MenuItemRepository;
import pe.com.salon.salongestionapi.menu.repository.AccesoMainRepository;
import pe.com.salon.salongestionapi.menu.entity.AccesoMain;
import pe.com.salon.salongestionapi.security.entity.Usuario;
import pe.com.salon.salongestionapi.security.repository.UsuarioRepository;
import pe.com.salon.salongestionapi.perfil_modulo.repository.PerfilModuloRepository;
import pe.com.salon.salongestionapi.perfil_modulo.repository.PerfilModuloMenuRepository;
import pe.com.salon.salongestionapi.perfil_modulo.entity.PerfilModulo;
import pe.com.salon.salongestionapi.perfil_modulo.entity.PerfilModuloMenu;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuService {

    private final MenuItemRepository menuItemRepository;
    private final UsuarioRepository UsuarioRepository;
    private final AccesoMainRepository accesoMainRepository;
    private final PerfilModuloRepository perfilModuloRepository;
    private final PerfilModuloMenuRepository perfilModuloMenuRepository;

    /**
     * Obtiene el menú completo filtrado según el sistema híbrido de acceso
     */
    @Transactional(readOnly = true)
    public List<MenuItemDTO> obtenerMenuParaUsuario(Long usuarioId) {
        return obtenerMenuParaUsuario(usuarioId, null);
    }

    /**
     * Obtiene el menú filtrado por portal
     */
    @Transactional(readOnly = true)
    public List<MenuItemDTO> obtenerMenuParaUsuario(Long usuarioId, Long accesoId) {
        log.debug("Obteniendo menú para usuario ID: {} y acceso ID: {}", usuarioId, accesoId);

        // Obtener usuario con sus permisos
        Usuario usuario = UsuarioRepository.findByIdWithRoles(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Obtener permisos efectivos del usuario
        Set<String> permisosUsuario = new java.util.HashSet<>(usuario.getPermissionNames());
        log.debug("Usuario {} tiene {} permisos", usuarioId, permisosUsuario.size());

        // Extraer módulos de los permisos
        Set<String> modulosUsuario = extraerModulos(permisosUsuario);

        // Verificar si es admin
        boolean esAdmin = usuario.getRoles().stream()
                .anyMatch(rol -> {
                    String name = rol.getName().toUpperCase();
                    return name.contains("ADMIN") || name.contains("ADMINISTRADOR");
                });

        if (esAdmin) {
            log.debug("Usuario {} es administrador, mostrando menú completo", usuarioId);
        }

        // Buscar el portal de Rehabilitación en la base de datos
        AccesoMain rehabPortal = accesoMainRepository.findAll().stream()
                .filter(a -> a.getNombre().toUpperCase().contains("REHAB"))
                .findFirst()
                .orElse(null);

        Long accesoFinalId = (rehabPortal != null) ? rehabPortal.getId() : accesoId;
        List<MenuItem> itemsRaiz = menuItemRepository.findRootItemsByAcceso(accesoFinalId);

        // REGLA DE COMPATIBILIDAD/FALLBACK:
        // Si no hay items para el accesoId, intentamos fallback a Rehabilitación de forma explícita
        if (itemsRaiz.isEmpty()) {
            log.debug("No se encontraron items para acceso {}. Intentando fallback...", accesoFinalId);

            if (rehabPortal != null) {
                log.info("Fallback exitoso a portal: {} (ID: {})", rehabPortal.getNombre(), rehabPortal.getId());
                accesoFinalId = rehabPortal.getId();
                itemsRaiz = menuItemRepository.findRootItemsByAcceso(accesoFinalId);
            }

            // ULTIMO RECURSO: Devolver lista vacía si no hay portal identificado
            if (itemsRaiz.isEmpty()) {
                log.warn("No se identificó ningún portal válido para el menú. Retornando vacío.");
                return new ArrayList<>();
            }
        }

        log.debug("Items raíz finales encontrados: {}", itemsRaiz.size());

        // =====================================================
        // FILTRO POR perfil_modulo_menu (accesos configurados)
        // Si el usuario tiene roles con menús asignados para este portal,
        // solo se muestran esos. Si no, se usan los permisos legacy.
        // =====================================================
        Set<Long> validIdsForAcceso = recolectarIds(itemsRaiz);
        Set<Long> allowedMenuIds = null;
        if (!esAdmin) {
            Set<Long> menuIds = new HashSet<>();
            boolean tieneConfiguracion = false;
            for (var rol : usuario.getRoles()) {
                List<PerfilModulo> perfiles = perfilModuloRepository.findByProfileIdAndIsDeletedFalse(rol.getId());
                for (PerfilModulo perfil : perfiles) {
                    List<PerfilModuloMenu> menuLinks = perfilModuloMenuRepository.findByPerfilModuloIdAndIsDeletedFalse(perfil.getId());
                    for (PerfilModuloMenu ml : menuLinks) {
                        if (ml.getMenuId() != null && validIdsForAcceso.contains(ml.getMenuId())) {
                            tieneConfiguracion = true;
                            menuIds.add(ml.getMenuId());
                        }
                    }
                }
            }
            if (tieneConfiguracion) {
                allowedMenuIds = menuIds;
                log.info("Filtrando menú por {} IDs configurados en perfil_modulo_menu para el portal", menuIds.size());
            }
        }

        // Construir árbol de menú filtrado
        List<MenuItemDTO> menu = new ArrayList<>();
        for (MenuItem item : itemsRaiz) {
            MenuItemDTO dto = construirMenuItemDTO(item, permisosUsuario, modulosUsuario, esAdmin, accesoFinalId, allowedMenuIds);
            if (dto != null && (dto.getChildren() == null || !dto.getChildren().isEmpty() || dto.getRuta() != null)) {
                menu.add(dto);
            }
        }

        log.debug("Menú construido con {} grupos principales", menu.size());
        return menu;
    }

    /**
     * Recolecta recursivamente todos los IDs pertenecientes a los ítems del portal actual
     */
    private Set<Long> recolectarIds(List<MenuItem> items) {
        Set<Long> ids = new HashSet<>();
        if (items != null) {
            for (MenuItem item : items) {
                if (item != null && item.getId() != null) {
                    ids.add(item.getId());
                    if (item.getChildren() != null && !item.getChildren().isEmpty()) {
                        ids.addAll(recolectarIds(item.getChildren()));
                    }
                }
            }
        }
        return ids;
    }

    /**
     * Extrae los módulos únicos de una lista de permisos
     */
    private Set<String> extraerModulos(Set<String> permisos) {
        return permisos.stream()
                .map(permiso -> {
                    if (permiso.contains("_")) {
                        return permiso.split("_")[0];
                    }
                    return permiso;
                })
                .collect(Collectors.toSet());
    }

    /**
     * Construye recursivamente el DTO del MenuItem con sistema híbrido
     * Sobrecarga sin allowedMenuIds (compatibilidad)
     */
    private MenuItemDTO construirMenuItemDTO(MenuItem item,
            Set<String> permisosUsuario,
            Set<String> modulosUsuario,
            boolean esAdmin,
            Long accesoId) {
        return construirMenuItemDTO(item, permisosUsuario, modulosUsuario, esAdmin, accesoId, null);
    }

    /**
     * Construye recursivamente el DTO del MenuItem con sistema híbrido
     * allowedMenuIds: si no es null, solo se incluyen ítems cuyo ID está en este Set
     */
    private MenuItemDTO construirMenuItemDTO(MenuItem item,
            Set<String> permisosUsuario,
            Set<String> modulosUsuario,
            boolean esAdmin,
            Long accesoId,
            Set<Long> allowedMenuIds) {

        // FILTRO POR perfil_modulo_menu: si hay IDs configurados, respetar exactamente esos
        boolean accesoConcedidoPorPerfil = false;
        if (allowedMenuIds != null) {
            if (!allowedMenuIds.contains(item.getId())) {
                return null;
            }
            // Si está en allowedMenuIds, el acceso está concedido explícitamente por el nuevo sistema
            accesoConcedidoPorPerfil = true;
        }

        // Si no está concedido por el nuevo sistema (allowedMenuIds es null), verificamos por el sistema legacy
        if (!accesoConcedidoPorPerfil && !tieneAccesoAlItem(item, permisosUsuario, modulosUsuario, esAdmin, accesoId)) {
            log.trace("❌ Usuario no tiene acceso al item: {} ({})",
                    item.getTitulo(), item.getDescripcionControl());
            return null;
        }

        // Crear DTO base
        MenuItemDTO dto = MenuItemDTO.builder()
                .id(item.getId())
                .titulo(item.getTitulo())
                .ruta(item.getRuta())
                .icono(item.getIcono())
                .orden(item.getOrden())
                .tipo(item.getTipo().name())
                .children(new ArrayList<>())
                .build();

        // Procesar hijos recursivamente
        if (item.getChildren() != null && !item.getChildren().isEmpty()) {
            for (MenuItem child : item.getChildren()) {
                MenuItemDTO childDto = construirMenuItemDTO(child, permisosUsuario, modulosUsuario, esAdmin, accesoId, allowedMenuIds);
                if (childDto != null) {
                    dto.getChildren().add(childDto);
                }
            }

            // Si es GRUPO o SUBMENU sin hijos visibles, no mostrarlo
            if (dto.getChildren().isEmpty() &&
                    (item.getTipo() == MenuItem.TipoMenuItem.GRUPO ||
                            item.getTipo() == MenuItem.TipoMenuItem.SUBMENU)) {
                log.trace("⚠️  Item {} no tiene hijos visibles, se omite", item.getTitulo());
                return null;
            }
        }

        log.trace("✅ Item incluido: {} ({})", item.getTitulo(), item.getDescripcionControl());
        return dto;
    }

    /**
     * ✨ SISTEMA HÍBRIDO: Verifica acceso según el tipo de control configurado y
     * aislamiento de portal
     */
    private boolean tieneAccesoAlItem(MenuItem item,
            Set<String> permisosUsuario,
            Set<String> modulosUsuario,
            boolean esAdmin,
            Long accesoId) {

        // REGLA DE AISLAMIENTO ESTRICTA: 
        // Si estamos en un portal específico, SOLO mostrar ítems de ese portal.
        // Se excluyen ítems de otros portales O ítems sin portal asignado (NULL) 
        // para evitar que el Administrador vea "fugas" de otros módulos.
        if (accesoId != null) {
            if (item.getAcceso() == null || !item.getAcceso().getId().equals(accesoId)) {
                return false;
            }
        }

        // Admin tiene acceso a todo dentro de su portal
        if (esAdmin) {
            return true;
        }

        // Si el item no requiere control, es público
        if (!item.requiereAcceso()) {
            log.trace("🔓 Item público: {}", item.getTitulo());
            return true;
        }

        // Verificar según el tipo de control
        switch (item.getTipoControl()) {
            case PUBLICO:
                return true;

            case MODULO:
                return verificarAccesoModulo(item, modulosUsuario);

            case PERMISOS:
                return verificarAccesoPermisosAny(item, permisosUsuario);

            case PERMISOS_TODOS:
                return verificarAccesoPermisosTodos(item, permisosUsuario);

            default:
                log.warn("⚠️  Tipo de control desconocido para item: {}", item.getTitulo());
                return false;
        }
    }

    /**
     * Verifica acceso por MÓDULO (cualquier permiso del módulo)
     */
    private boolean verificarAccesoModulo(MenuItem item, Set<String> modulosUsuario) {
        String moduloRequerido = item.getModuloRequerido();

        if (moduloRequerido == null || moduloRequerido.isEmpty()) {
            log.warn("⚠️  Item {} tiene tipo MODULO pero no especifica módulo", item.getTitulo());
            return false;
        }

        boolean tieneAcceso = modulosUsuario.contains(moduloRequerido);

        if (!tieneAcceso) {
            log.trace("🔒 No tiene módulo {} para {}", moduloRequerido, item.getTitulo());
        } else {
            log.trace("🔓 Tiene módulo {} para {}", moduloRequerido, item.getTitulo());
        }

        return tieneAcceso;
    }

    /**
     * Verifica acceso por PERMISOS (necesita AL MENOS UNO)
     */
    private boolean verificarAccesoPermisosAny(MenuItem item, Set<String> permisosUsuario) {
        List<String> permisosRequeridos = item.getPermisosRequeridosList();

        if (permisosRequeridos.isEmpty()) {
            log.warn("⚠️  Item {} tiene tipo PERMISOS pero no especifica permisos", item.getTitulo());
            return false;
        }

        boolean tieneAcceso = permisosRequeridos.stream()
                .anyMatch(permisosUsuario::contains);

        if (!tieneAcceso) {
            log.trace("🔒 No tiene ninguno de {} para {}", permisosRequeridos, item.getTitulo());
        } else {
            log.trace("🔓 Tiene al menos uno de {} para {}", permisosRequeridos, item.getTitulo());
        }

        return tieneAcceso;
    }

    /**
     * Verifica acceso por PERMISOS (necesita TODOS)
     */
    private boolean verificarAccesoPermisosTodos(MenuItem item, Set<String> permisosUsuario) {
        List<String> permisosRequeridos = item.getPermisosRequeridosList();

        if (permisosRequeridos.isEmpty()) {
            log.warn("⚠️  Item {} tiene tipo PERMISOS_TODOS pero no especifica permisos", item.getTitulo());
            return false;
        }

        boolean tieneAcceso = permisosRequeridos.stream()
                .allMatch(permisosUsuario::contains);

        if (!tieneAcceso) {
            List<String> faltantes = permisosRequeridos.stream()
                    .filter(p -> !permisosUsuario.contains(p))
                    .collect(Collectors.toList());
            log.trace("🔒 Le faltan {} de {} para {}", faltantes, permisosRequeridos, item.getTitulo());
        } else {
            log.trace("🔓 Tiene todos {} para {}", permisosRequeridos, item.getTitulo());
        }

        return tieneAcceso;
    }

    /**
     * Obtiene todos los items del menú (sin filtrar)
     */
    @Transactional(readOnly = true)
    public List<MenuItem> obtenerTodosLosItems() {
        return menuItemRepository.findAll();
    }

    /**
     * Obtiene los items raíz del menú
     */
    @Transactional(readOnly = true)
    public List<MenuItem> obtenerItemsRaiz() {
        return menuItemRepository.findRootItems();
    }

    /**
     * Obtiene el árbol completo de menús sin filtrar por permisos
     * Usado para la gestión de accesos por rol (modal de Accesos)
     */
    @Transactional(readOnly = true)
    public List<MenuItemDTO> obtenerArbolCompleto(Long accesoId) {
        log.info("📋 Obteniendo árbol completo de menús para acceso ID: {}", accesoId);
        
        // Buscar el portal de Rehabilitación en la base de datos
        AccesoMain rehabPortal = accesoMainRepository.findAll().stream()
                .filter(a -> a.getNombre().toUpperCase().contains("REHAB"))
                .findFirst()
                .orElse(null);

        Long finalAccesoId = (rehabPortal != null) ? rehabPortal.getId() : accesoId;
        List<MenuItem> raices = null;
        if (finalAccesoId != null) {
            raices = menuItemRepository.findRootItemsByAcceso(finalAccesoId);
        }
        
        // REGLA DE COMPATIBILIDAD/FALLBACK:
        // Si no se encontraron items, intentamos fallback a Rehabilitación de forma explícita
        if (raices == null || raices.isEmpty()) {
            log.debug("No se encontraron items para acceso {}. Intentando fallback...", finalAccesoId);
            
            if (rehabPortal != null) {
                log.info("Fallback exitoso a portal: {} (ID: {})", rehabPortal.getNombre(), rehabPortal.getId());
                raices = menuItemRepository.findRootItemsByAcceso(rehabPortal.getId());
            }
        }
        
        // ULTIMO RECURSO: Devolver lista de todas las raíces si sigue siendo vacío o nulo
        if (raices == null || raices.isEmpty()) {
            log.warn("No se identificó ningún portal válido para el menú completo. Usando findRootItems.");
            raices = menuItemRepository.findRootItems();
        }

        List<MenuItemDTO> resultado = new ArrayList<>();
        for (MenuItem item : raices) {
            resultado.add(construirDtoSinFiltro(item));
        }
        return resultado;
    }

    private MenuItemDTO construirDtoSinFiltro(MenuItem item) {
        MenuItemDTO dto = MenuItemDTO.builder()
                .id(item.getId())
                .titulo(item.getTitulo())
                .ruta(item.getRuta())
                .icono(item.getIcono())
                .orden(item.getOrden())
                .tipo(item.getTipo().name())
                .children(new ArrayList<>())
                .build();
        if (item.getChildren() != null) {
            for (MenuItem child : item.getChildren()) {
                dto.getChildren().add(construirDtoSinFiltro(child));
            }
        }
        return dto;
    }
}
