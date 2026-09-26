package pe.com.salon.salongestionapi.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.Collections;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // Permite cualquier origen (útil para desarrollo)
        config.setAllowedOriginPatterns(Collections.singletonList("*"));
        
        // Permite todos los métodos HTTP (GET, POST, PUT, DELETE, OPTIONS, etc)
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Permite cualquier cabecera
        config.setAllowedHeaders(Collections.singletonList("*"));
        
        // Permite envío de credenciales (cookies, headers de autorización)
        config.setAllowCredentials(true);
        
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
