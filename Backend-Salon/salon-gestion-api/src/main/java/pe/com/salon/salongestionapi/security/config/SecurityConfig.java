package pe.com.salon.salongestionapi.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import pe.com.salon.salongestionapi.auth.service.JwtAuthenticationFilter;
import pe.com.salon.salongestionapi.security.service.CustomUserDetailsService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // Roles con acceso total (mismo criterio que permissionGuard del frontend)
    private static final String[] ADMIN_ROLES = {"ROLE_ADMIN", "ROLE_ADMINISTRADOR"};

    // Métodos que modifican datos. Las lecturas (GET) quedan abiertas a cualquier usuario autenticado
    // porque varias pantallas (POS, Agenda, Dashboard) consultan catálogos de otros módulos.
    private static final HttpMethod[] WRITE_METHODS = {
            HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE
    };

    // Sub-módulos tal como se crean en DataSeeder (VER_/CREAR_/ACTUALIZAR_/ELIMINAR_ + nombre)
    private static final String AGENDA = "AGENDA_Y_CITAS";
    private static final String POS = "PUNTO_DE_VENTA_POS";
    private static final String ASISTENCIA = "CONTROL_DE_ASISTENCIA";
    private static final String EMPLEADOS = "GESTION_DE_EMPLEADOS";
    private static final String ESPECIALIDADES = "ESPECIALIDADES";
    private static final String TURNOS = "TURNOS";
    private static final String COMISIONES = "COMISIONES";
    private static final String INCENTIVOS = "CAMPANAS_E_INCENTIVOS";
    private static final String MONITOR_COMISIONES = "MONITOR_DE_COMISIONES";
    private static final String SERVICIOS = "SERVICIOS";
    private static final String PRODUCTOS = "PRODUCTOS";
    private static final String CATEGORIAS = "CATEGORIAS";
    private static final String INVENTARIO = "CONTROL_DE_INVENTARIO";
    private static final String CAJA = "CAJA";
    private static final String FACTURACION = "FACTURACION";
    private static final String EGRESOS = "EGRESOS";
    private static final String LIQUIDACIONES = "LIQUIDACIONES";

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins:http://localhost:4200,http://127.0.0.1:4200}")
    private String allowedOrigins;

    public SecurityConfig(CustomUserDetailsService userDetailsService, JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Sin token (o token inválido) => 401, así el frontend distingue "sesión caída" de "sin permiso" (403)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"success\": false, \"message\": \"No autenticado.\"}");
                })
                // Se responde aquí mismo: si se reenvía a /error, esa ruta corre sin token y termina en 401
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"success\": false, \"message\": \"No tiene permiso para realizar esta acción.\"}");
                }))
            .authorizeHttpRequests(auth -> {
                // Página de error interna de Spring (evita que un 400/404/500 se convierta en 401)
                auth.requestMatchers("/error").permitAll();
                // Rutas públicas que no requieren token
                auth.requestMatchers("/api/auth/login", "/api/v1/empresa/activa").permitAll();
                // Kiosko de asistencia: pantalla pública (/kiosko) donde el personal marca con su DNI
                auth.requestMatchers(HttpMethod.POST, "/api/rrhh/asistencias/kiosko").permitAll();
                auth.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll();

                // Alta de usuarios: sólo quien puede administrar usuarios/roles
                auth.requestMatchers("/api/auth/register").hasAnyAuthority(withAdmin("ROL_CREAR"));

                // Configuración global (empresa, tiempo de sesión) y perfiles de módulo: sólo administrador
                for (HttpMethod method : WRITE_METHODS) {
                    auth.requestMatchers(method, "/api/v1/empresa/**", "/api/admin/configuracion/**").hasAnyAuthority(ADMIN_ROLES);
                    auth.requestMatchers(method, "/api/perfil-modulo/**", "/api/perfil-modulo-menu/**")
                            .hasAnyAuthority(withAdmin("ROL_ACTUALIZAR"));
                }

                // Escrituras por módulo: el usuario debe tener algún permiso del módulo (o de uno que lo use)
                writeRule(auth, new String[]{"/api/v1/categorias/**"}, CATEGORIAS);
                writeRule(auth, new String[]{"/api/v1/servicios/**"}, SERVICIOS);
                writeRule(auth, new String[]{"/api/v1/productos/**"}, PRODUCTOS, INVENTARIO);
                writeRule(auth, new String[]{"/api/operaciones/citas/**"}, AGENDA);
                writeRule(auth, new String[]{"/api/operaciones/clientes/**"}, AGENDA, POS, CAJA);
                auth.requestMatchers(HttpMethod.DELETE, "/api/operaciones/tickets/**").hasAnyAuthority(ADMIN_ROLES);
                auth.requestMatchers(HttpMethod.PUT, "/api/operaciones/tickets/*/precios").hasAnyAuthority(ADMIN_ROLES);
                writeRule(auth, new String[]{"/api/operaciones/tickets/**"}, POS, CAJA, FACTURACION);
                writeRule(auth, new String[]{"/api/operaciones/caja/**"}, CAJA);
                writeRule(auth, new String[]{"/api/operaciones/egresos/**", "/api/finanzas/gastos/**"}, EGRESOS, CAJA);
                writeRule(auth, new String[]{"/api/fidelizacion/**"}, SERVICIOS, POS, CAJA);
                writeRule(auth, new String[]{"/api/rrhh/asistencias/**"}, ASISTENCIA);
                writeRule(auth, new String[]{"/api/rrhh/empleados/**"}, EMPLEADOS);
                writeRule(auth, new String[]{"/api/rrhh/especialidades/**"}, ESPECIALIDADES);
                writeRule(auth, new String[]{"/api/rrhh/turnos/**"}, TURNOS);
                writeRule(auth, new String[]{"/api/v1/comisiones/**", "/api/finanzas/tramos-comision/**"}, COMISIONES);
                writeRule(auth, new String[]{"/api/v1/incentivos/**"}, INCENTIVOS, MONITOR_COMISIONES);
                writeRule(auth, new String[]{"/api/rrhh/liquidaciones/**", "/api/finanzas/liquidaciones/**"}, LIQUIDACIONES);

                // El resto de rutas requieren token (usuarios/roles/permisos se controlan con @PreAuthorize)
                auth.anyRequest().authenticated();
            })
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static void writeRule(
            org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
            String[] paths, String... subModules) {
        String[] authorities = moduleAuthorities(subModules);
        for (HttpMethod method : WRITE_METHODS) {
            auth.requestMatchers(method, paths).hasAnyAuthority(authorities);
        }
    }

    private static String[] moduleAuthorities(String... subModules) {
        List<String> authorities = new ArrayList<>();
        for (String subModule : subModules) {
            for (String action : new String[]{"VER_", "CREAR_", "ACTUALIZAR_", "ELIMINAR_"}) {
                authorities.add(action + subModule);
            }
        }
        return withAdmin(authorities.toArray(new String[0]));
    }

    private static String[] withAdmin(String... authorities) {
        List<String> all = new ArrayList<>(Arrays.asList(ADMIN_ROLES));
        all.addAll(Arrays.asList(authorities));
        return all.toArray(new String[0]);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // En Docker/tablets el frontend llega por Nginx en el mismo origen (/api), así que CORS sólo
        // hace falta para 'ng serve'. Para agregar orígenes usar APP_CORS_ALLOWED_ORIGINS (separados por coma).
        configuration.setAllowedOriginPatterns(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
