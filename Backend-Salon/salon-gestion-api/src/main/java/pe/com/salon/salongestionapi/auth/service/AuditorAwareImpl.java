package pe.com.salon.salongestionapi.auth.service; 

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || 
                !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.of("SYSTEM");
            }

            return Optional.ofNullable(authentication.getName());
        } catch (Exception e) {
            // Si ocurre cualquier error accediendo al contexto de seguridad
            // (como el error de "No thread-bound request"), retornamos SYSTEM
            return Optional.of("SYSTEM");
        }
    }
}
