package pe.com.salon.salongestionapi.perfil_modulo.service;

import pe.com.salon.salongestionapi.menu.dto.MenuItemDTO;
import pe.com.salon.salongestionapi.menu.entity.MenuItem;
import pe.com.salon.salongestionapi.menu.repository.MenuItemRepository;
import pe.com.salon.salongestionapi.perfil_modulo.entity.Modulo;
import pe.com.salon.salongestionapi.perfil_modulo.entity.PerfilModulo;
import pe.com.salon.salongestionapi.perfil_modulo.entity.PerfilModuloMenu;
import pe.com.salon.salongestionapi.perfil_modulo.repository.ModuloRepository;
import pe.com.salon.salongestionapi.perfil_modulo.repository.PerfilModuloMenuRepository;
import pe.com.salon.salongestionapi.perfil_modulo.repository.PerfilModuloRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerfilModuloService {

    private final PerfilModuloRepository perfilModuloRepository;
    private final PerfilModuloMenuRepository perfilModuloMenuRepository;
    private final ModuloRepository moduloRepository;
    private final MenuItemRepository menuItemRepository;
    private final JdbcTemplate jdbcTemplate;

    // ================================================
    // MÓDULOS
    // ================================================

    @Transactional(readOnly = true)
    public List<Modulo> getAllModulos() {
        return moduloRepository.findAll();
    }

    // ================================================
    // PERFIL-MÓDULO
    // ================================================

    @Transactional(readOnly = true)
    public List<PerfilModulo> getByRoleId(Long roleId) {
        return perfilModuloRepository.findByProfileIdAndIsDeletedFalse(roleId);
    }

    /**
     * Vincula un rol con uno o más módulos. Si ya existe el vínculo, lo retorna.
     * Body esperado: lista de módulos con al menos { id: Long }
     * Retorna el primer PerfilModulo creado/existente.
     */
    @Transactional
    public PerfilModulo linkRoleToModulos(Long roleId, List<Map<String, Object>> modulos) {
        PerfilModulo first = null;
        for (Map<String, Object> mod : modulos) {
            Long moduloId = toLong(mod.get("id"));
            if (moduloId == null) continue;

            PerfilModulo existing = perfilModuloRepository
                    .findByProfileIdAndModuloIdAndIsDeletedFalse(roleId, moduloId)
                    .orElse(null);

            if (existing != null) {
                if (first == null) first = existing;
                continue;
            }

            PerfilModulo nuevo = PerfilModulo.builder()
                    .profileId(roleId)
                    .moduloId(moduloId)
                    .isDeleted(false)
                    .createdBy("SYSTEM")
                    .build();
            PerfilModulo saved = perfilModuloRepository.save(nuevo);
            if (first == null) first = saved;
        }
        // Fallback: si modulos está vacío, retornar el primero existente para el rol
        if (first == null) {
            List<PerfilModulo> existing = perfilModuloRepository.findByProfileIdAndIsDeletedFalse(roleId);
            if (!existing.isEmpty()) first = existing.get(0);
        }
        return first;
    }

    // ================================================
    // PERFIL-MÓDULO-MENU
    // ================================================

    /**
     * Retorna los MenuItemDTO asignados a un perfil_modulo (para inicializar los checks).
     */
    @Transactional(readOnly = true)
    public List<MenuItemDTO> getMenusByPerfilModuloId(Long perfilModuloId) {
        List<PerfilModuloMenu> links = perfilModuloMenuRepository.findByPerfilModuloIdAndIsDeletedFalse(perfilModuloId);
        if (links.isEmpty()) return new ArrayList<>();

        List<Long> menuIds = links.stream().map(PerfilModuloMenu::getMenuId).collect(Collectors.toList());
        List<MenuItem> items = menuItemRepository.findAllById(menuIds);

        return items.stream().map(item -> MenuItemDTO.builder()
                .id(item.getId())
                .titulo(item.getTitulo())
                .ruta(item.getRuta())
                .icono(item.getIcono())
                .orden(item.getOrden())
                .tipo(item.getTipo().name())
                .children(new ArrayList<>())
                .build()
        ).collect(Collectors.toList());
    }

    /**
     * Reemplaza todos los menús asignados a un perfil_modulo.
     * Body esperado: lista de { id: Long } (menu item ids).
     */
    @Transactional
    public void saveMenusForPerfilModulo(Long perfilModuloId, List<Map<String, Object>> menus) {
        log.info("💾 Guardando {} menús para perfilModuloId={}", menus.size(), perfilModuloId);

        try {
            jdbcTemplate.execute("ALTER TABLE perfil_modulo_menu DROP CONSTRAINT IF EXISTS fk6048ns71efrigdt0e5866ig80;");
            jdbcTemplate.execute("DO $$ DECLARE r RECORD; BEGIN FOR r IN (SELECT c.conname FROM pg_constraint c JOIN pg_class rel ON rel.oid = c.conrelid JOIN pg_class frel ON frel.oid = c.confrelid WHERE rel.relname = 'perfil_modulo_menu' AND frel.relname IN ('menu', 'menu_items') AND c.contype = 'f') LOOP EXECUTE 'ALTER TABLE perfil_modulo_menu DROP CONSTRAINT IF EXISTS ' || r.conname; END LOOP; END $$;");
        } catch (Exception e) {
            log.warn("No se pudieron limpiar constraints legacy en perfil_modulo_menu: {}", e.getMessage());
        }

        // Borrar los existentes
        perfilModuloMenuRepository.deleteByPerfilModuloId(perfilModuloId);

        // Insertar los nuevos
        List<PerfilModuloMenu> nuevos = new ArrayList<>();
        for (Map<String, Object> menu : menus) {
            Long menuId = toLong(menu.get("id"));
            if (menuId == null) continue;
            nuevos.add(PerfilModuloMenu.builder()
                    .perfilModuloId(perfilModuloId)
                    .menuId(menuId)
                    .isDeleted(false)
                    .createdBy("SYSTEM")
                    .build());
        }
        perfilModuloMenuRepository.saveAll(nuevos);
        log.info("✅ Guardados {} vínculos perfil_modulo_menu", nuevos.size());
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(value.toString()); } catch (Exception e) { return null; }
    }
}
