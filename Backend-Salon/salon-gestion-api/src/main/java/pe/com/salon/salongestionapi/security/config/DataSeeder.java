package pe.com.salon.salongestionapi.security.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import pe.com.salon.salongestionapi.menu.entity.AccesoMain;
import pe.com.salon.salongestionapi.menu.repository.AccesoMainRepository;
import pe.com.salon.salongestionapi.permissions.entity.Permission;
import pe.com.salon.salongestionapi.permissions.repository.PermissionRepository;
import pe.com.salon.salongestionapi.roles.entity.Role;
import pe.com.salon.salongestionapi.roles.repository.RoleRepository;
import pe.com.salon.salongestionapi.security.entity.Usuario;
import pe.com.salon.salongestionapi.security.repository.UsuarioRepository;
import pe.com.salon.salongestionapi.catalogo.entity.Categoria;
import pe.com.salon.salongestionapi.catalogo.entity.Servicio;
import pe.com.salon.salongestionapi.catalogo.entity.Producto;
import pe.com.salon.salongestionapi.catalogo.entity.Categoria;
import pe.com.salon.salongestionapi.catalogo.entity.Servicio;
import pe.com.salon.salongestionapi.catalogo.repository.CategoriaRepository;
import pe.com.salon.salongestionapi.catalogo.repository.ServicioRepository;
import pe.com.salon.salongestionapi.catalogo.repository.ProductoRepository;
import pe.com.salon.salongestionapi.rrhh.entity.Especialidad;
import pe.com.salon.salongestionapi.rrhh.repository.EspecialidadRepository;
import java.math.BigDecimal;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import org.springframework.jdbc.core.JdbcTemplate;
// ... (I will add it inside the class instead)

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final AccesoMainRepository accesoMainRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final pe.com.salon.salongestionapi.empresa.repository.EmpresaRepository empresaRepository;
    private final JdbcTemplate jdbcTemplate;
    private final CategoriaRepository categoriaRepository;
    private final ServicioRepository servicioRepository;
    private final ProductoRepository productoRepository;
    private final EspecialidadRepository especialidadRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Iniciando Data Seeder...");

        try {
            jdbcTemplate.execute("ALTER TABLE empresas ALTER COLUMN logo_url TYPE TEXT;");
            log.info("Migración de logo_url a TEXT ejecutada correctamente.");
        } catch (Exception e) {
            log.warn("No se pudo alterar/agregar columnas o ya existían: {}", e.getMessage());
        }

        // 1. Crear Accesos Main (Portales) si no existen
        AccesoMain portalAdmin = createAccesoMainIfNotFound("Portal Administrativo");
        AccesoMain portalSalon = createAccesoMainIfNotFound("Portal Salón");

        // 2. Crear Permisos (Permissions) Granulares (CRUD)
        
        // 2. Crear Permisos (Permissions) Granulares por Sub-Módulo
        Set<Permission> adminPerms = new HashSet<>();
        
        // Módulo Operaciones
        createCrudPermissions("Agenda y Citas", "Operaciones", portalSalon, adminPerms);
        createCrudPermissions("Punto de Venta POS", "Operaciones", portalSalon, adminPerms);

        // Módulo RRHH
        createCrudPermissions("Control de Asistencia", "RRHH", portalAdmin, adminPerms);
        createCrudPermissions("Gestión de Empleados", "RRHH", portalAdmin, adminPerms);
        createCrudPermissions("Especialidades", "RRHH", portalAdmin, adminPerms);
        createCrudPermissions("Turnos", "RRHH", portalAdmin, adminPerms);
        createCrudPermissions("Comisiones", "RRHH", portalAdmin, adminPerms);
        createCrudPermissions("Campañas e Incentivos", "RRHH", portalAdmin, adminPerms);
        createCrudPermissions("Monitor de Comisiones", "RRHH", portalAdmin, adminPerms);

        // Módulo Catálogo
        createCrudPermissions("Servicios", "Catálogo", portalAdmin, adminPerms);
        createCrudPermissions("Productos", "Catálogo", portalAdmin, adminPerms);
        createCrudPermissions("Categorías", "Catálogo", portalAdmin, adminPerms);
        createCrudPermissions("Control de Inventario", "Catálogo", portalAdmin, adminPerms);

        // Módulo Finanzas
        createCrudPermissions("Caja", "Finanzas", portalAdmin, adminPerms);
        createCrudPermissions("Facturación", "Finanzas", portalAdmin, adminPerms);
        createCrudPermissions("Egresos", "Finanzas", portalAdmin, adminPerms);
        createCrudPermissions("Liquidaciones", "Finanzas", portalAdmin, adminPerms);

        // Módulo Analítica & Seguridad
        createCrudPermissions("Reportes de Rendimiento", "Analítica y Seguridad", portalAdmin, adminPerms);
        createCrudPermissions("Usuarios", "Analítica y Seguridad", portalAdmin, adminPerms);
        createCrudPermissions("Roles y Permisos", "Analítica y Seguridad", portalAdmin, adminPerms);

        // 3. Crear Roles y asignar permisos
        Role roleAdmin = createRoleIfNotFound("ADMIN", "Administrador Total del Sistema", portalAdmin, adminPerms);
            
        // Extraer algunos permisos específicos para Cajero
        Set<Permission> cajeroPerms = new HashSet<>();
        permissionRepository.findAll().forEach(p -> {
            if (p.getModule().contains("Punto de Venta POS") || p.getModule().contains("Caja") || p.getModule().contains("Facturación")) {
                if (p.getName().startsWith("VER_") || p.getName().startsWith("CREAR_")) {
                    cajeroPerms.add(p);
                }
            }
        });
        Role roleCajero = createRoleIfNotFound("CAJERO", "Cobros y Punto de Venta", portalSalon, cajeroPerms);
            
        // Extraer algunos permisos para Barbero
        Set<Permission> barberoPerms = new HashSet<>();
        permissionRepository.findAll().forEach(p -> {
            if (p.getModule().contains("Agenda y Citas")) {
                if (p.getName().startsWith("VER_") || p.getName().startsWith("CREAR_") || p.getName().startsWith("ACTUALIZAR_")) {
                    barberoPerms.add(p);
                }
            }
        });
        Role roleBarbero = createRoleIfNotFound("BARBERO", "Especialista del Salón", portalSalon, barberoPerms);

        // 4. Crear Usuarios por Defecto
        createUserIfNotFound("admin@salon.com", "admin123", Set.of(roleAdmin));
        createUserIfNotFound("caja@salon.com", "caja123", Set.of(roleCajero));
        createUserIfNotFound("juan@salon.com", "juan123", Set.of(roleBarbero));

        // 5. Crear Empresa por Defecto
        createEmpresaIfNotFound("Mi Salón VIP", "Mi Salón S.A.C.", "20123456789");

        // 6. Crear Categorías, Especialidades y Servicios
        Categoria catCabello = createCategoriaIfNotFound("Cuidado Capilar", "Servicios para el cabello");
        Categoria catUnas = createCategoriaIfNotFound("Manicure y Pedicure", "Cuidado de uñas");
        Categoria catMaquillaje = createCategoriaIfNotFound("Maquillaje", "Maquillaje profesional");

        Especialidad espEstilista = createEspecialidadIfNotFound("Estilista", "Cortes y Peinados");
        Especialidad espManicurista = createEspecialidadIfNotFound("Manicurista", "Uñas");
        Especialidad espMaquillador = createEspecialidadIfNotFound("Maquillador", "Maquillaje");

        createServicioIfNotFound("Corte de Mujer", "Corte clásico o moderno", new BigDecimal("35.00"), 45, catCabello, espEstilista);
        createServicioIfNotFound("Corte de Hombre", "Corte con máquina o tijera", new BigDecimal("25.00"), 30, catCabello, espEstilista);
        createServicioIfNotFound("Manicure Rusa", "Limpieza profunda con torno", new BigDecimal("45.00"), 60, catUnas, espManicurista);
        createServicioIfNotFound("Uñas Acrílicas", "Uñas esculpidas en acrílico", new BigDecimal("80.00"), 120, catUnas, espManicurista);
        createServicioIfNotFound("Maquillaje de Noche", "Maquillaje social para eventos", new BigDecimal("60.00"), 60, catMaquillaje, espMaquillador);

        try {
            jdbcTemplate.execute("ALTER TABLE producto ALTER COLUMN image_url TYPE TEXT");
        } catch (Exception e) {
            log.warn("No se pudo alterar la columna image_url: " + e.getMessage());
        }

        // 7. Crear Productos con imágenes en Base64 si no existen
        createProductoIfNotFound("Shampoo Sin Sal 1L", "L'Oreal", "SKU-CAP-001", "ProHair Import", getBase64Image("shampoo.jpg"), new BigDecimal("25.00"), new BigDecimal("45.00"), 50, 10, true, true, catCabello);
        createProductoIfNotFound("Keratina Alisante 500ml", "Kativa", "SKU-CAP-002", "ProHair Import", getBase64Image("keratina.jpg"), new BigDecimal("40.00"), new BigDecimal("0.00"), 20, 5, true, false, catCabello);
        createProductoIfNotFound("Esmalte Rojo Clásico", "OPI", "SKU-UNA-001", "BeautyCorp Latam", getBase64Image("esmalte.jpg"), new BigDecimal("15.00"), new BigDecimal("35.00"), 30, 5, true, true, catUnas);
        createProductoIfNotFound("Aceite de Cutículas", "OPI", "SKU-UNA-002", "BeautyCorp Latam", getBase64Image("aceite.jpg"), new BigDecimal("10.00"), new BigDecimal("25.00"), 40, 10, true, true, catUnas);

        // Actualizar imágenes nulas (por si falló la carga anterior y el usuario no ha subido una propia)
        productoRepository.findAll().forEach(p -> {
            if (p.getImageUrl() == null) {
                if (p.getNombre().contains("Shampoo")) p.setImageUrl(getBase64Image("shampoo.jpg"));
                else if (p.getNombre().contains("Keratina")) p.setImageUrl(getBase64Image("keratina.jpg"));
                else if (p.getNombre().contains("Esmalte")) p.setImageUrl(getBase64Image("esmalte.jpg"));
                else if (p.getNombre().contains("Aceite")) p.setImageUrl(getBase64Image("aceite.jpg"));
                productoRepository.save(p);
            }
        });

        log.info("Data Seeder finalizado correctamente.");
    }

    private void createEmpresaIfNotFound(String nombreComercial, String razonSocial, String ruc) {
        if (empresaRepository.findFirstByActivoTrue().isEmpty()) {
            pe.com.salon.salongestionapi.empresa.entity.Empresa empresa = pe.com.salon.salongestionapi.empresa.entity.Empresa.builder()
                    .nombreComercial(nombreComercial)
                    .razonSocial(razonSocial)
                    .ruc(ruc)
                    .direccion("Av. Principal 123, Ciudad")
                    .telefono("+123456789")
                    .email("contacto@misalon.com")
                    .activo(true)
                    .build();
            empresaRepository.save(empresa);
        }
    }

    private AccesoMain createAccesoMainIfNotFound(String nombre) {
        List<AccesoMain> accesos = accesoMainRepository.findAll();
        for (AccesoMain acceso : accesos) {
            if (acceso.getNombre().equalsIgnoreCase(nombre)) {
                return acceso;
            }
        }
        AccesoMain newAcceso = AccesoMain.builder().nombre(nombre).build();
        return accesoMainRepository.save(newAcceso);
    }

    private Permission createPermissionIfNotFound(String name, String module, AccesoMain acceso) {
        return permissionRepository.findAll().stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElseGet(() -> permissionRepository.save(
                        Permission.builder()
                                .name(name)
                                .description("Permiso para " + name)
                                .module(module)
                                .acceso(acceso)
                                .active(true)
                                .build()
                ));
    }

    private Role createRoleIfNotFound(String name, String description, AccesoMain acceso, Set<Permission> permissions) {
        return roleRepository.findAll().stream()
                .filter(r -> r.getName().equals(name))
                .findFirst()
                .orElseGet(() -> {
                    Role role = Role.builder()
                            .name(name)
                            .description(description)
                            .acceso(acceso)
                            .active(true)
                            .build();
                    // Importante: Setear los permisos antes de guardar para persistir la tabla intermedia
                    role.setPermissions(new HashSet<>(permissions));
                    return roleRepository.save(role);
                });
    }

    private Usuario createUserIfNotFound(String email, String password, Set<Role> roles) {
        Optional<Usuario> userOpt = usuarioRepository.findByUsername(email);
        if (userOpt.isPresent()) {
            return userOpt.get();
        }

        Usuario usuario = Usuario.builder()
                .username(email)
                .email(email)
                .password(passwordEncoder.encode(password))
                .estado(true)
                .build();
                
        usuario.setRoles(new HashSet<>(roles));
        return usuarioRepository.save(usuario);
    }

    private Categoria createCategoriaIfNotFound(String nombre, String descripcion) {
        return categoriaRepository.findAll().stream()
                .filter(c -> c.getNombre().equals(nombre))
                .findFirst()
                .orElseGet(() -> categoriaRepository.save(Categoria.builder()
                        .nombre(nombre)
                        .descripcion(descripcion)
                        .estado(true)
                        .build()));
    }

    private Especialidad createEspecialidadIfNotFound(String nombre, String descripcion) {
        return especialidadRepository.findAll().stream()
                .filter(e -> e.getNombre().equals(nombre))
                .findFirst()
                .orElseGet(() -> {
                    Especialidad esp = new Especialidad();
                    esp.setNombre(nombre);
                    esp.setDescripcion(descripcion);
                    esp.setEstado(true);
                    return especialidadRepository.save(esp);
                });
    }

    private void createServicioIfNotFound(String nombre, String descripcion, BigDecimal precio, Integer duracion, Categoria categoria, Especialidad especialidad) {
        if (servicioRepository.findAll().stream().noneMatch(s -> s.getNombre().equals(nombre))) {
            Servicio servicio = new Servicio();
            servicio.setNombre(nombre);
            servicio.setDescripcion(descripcion);
            servicio.setPrecioBase(precio);
            servicio.setDuracionMinutos(duracion);
            servicio.setCategoria(categoria);
            servicio.setEspecialidadRequerida(especialidad);
            servicio.setEstado(true);
            servicioRepository.save(servicio);
        }
    }

    private void createProductoIfNotFound(String nombre, String marca, String sku, String proveedor, String imageUrl, BigDecimal costo, BigDecimal precioVenta, Integer stockActual, Integer stockMinimo, Boolean usoInterno, Boolean ventaDirecta, Categoria categoria) {
        if (productoRepository.findAll().stream().noneMatch(p -> p.getNombre().equals(nombre))) {
            Producto producto = new Producto();
            producto.setNombre(nombre);
            producto.setMarca(marca);
            producto.setSku(sku);
            producto.setProveedor(proveedor);
            producto.setImageUrl(imageUrl);
            producto.setCosto(costo);
            producto.setPrecioVenta(precioVenta);
            producto.setStockActual(stockActual);
            producto.setStockMinimo(stockMinimo);
            producto.setUsoInterno(usoInterno);
            producto.setVentaDirecta(ventaDirecta);
            producto.setCategoria(categoria);
            producto.setEstado(true);
            productoRepository.save(producto);
        }
    }

    private String getBase64Image(String filename) {
        try {
            String path = "../../FrontEnd-Salon/public/assets/productos/" + filename;
            byte[] fileContent = Files.readAllBytes(Paths.get(path));
            String base64 = Base64.getEncoder().encodeToString(fileContent);
            return "data:image/jpeg;base64," + base64;
        } catch (Exception e) {
            log.warn("No se pudo cargar la imagen {} a base64: {}", filename, e.getMessage());
            return null; // Si no lo encuentra, quedará null y usará el ui-avatars por defecto
        }
    }

    private void createCrudPermissions(String subModule, String moduleName, AccesoMain acceso, Set<Permission> adminPerms) {
        String baseName = subModule.toUpperCase()
                .replace(" ", "_")
                .replace("Y", "Y")
                .replace("Ñ", "N")
                .replace("Á", "A").replace("É", "E").replace("Í", "I").replace("Ó", "O").replace("Ú", "U");

        String fullModuleName = moduleName + " - " + subModule;
        
        Permission ver = createPermissionIfNotFound("VER_" + baseName, fullModuleName, acceso);
        Permission crear = createPermissionIfNotFound("CREAR_" + baseName, fullModuleName, acceso);
        Permission act = createPermissionIfNotFound("ACTUALIZAR_" + baseName, fullModuleName, acceso);
        Permission elim = createPermissionIfNotFound("ELIMINAR_" + baseName, fullModuleName, acceso);

        adminPerms.add(ver);
        adminPerms.add(crear);
        adminPerms.add(act);
        adminPerms.add(elim);
    }
}
