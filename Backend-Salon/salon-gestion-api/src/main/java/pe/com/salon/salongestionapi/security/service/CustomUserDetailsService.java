package pe.com.salon.salongestionapi.security.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.salon.salongestionapi.security.entity.Usuario;
import pe.com.salon.salongestionapi.security.repository.UsuarioRepository;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con el username: " + username));

        Set<GrantedAuthority> authorities = new HashSet<>();
        
        // Agregar roles y permisos
        usuario.getRoles().forEach(role -> {
            String roleName = role.getName().toUpperCase();
            // Agregar el rol
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
            
            // Agregar los permisos asociados a ese rol (desde la BD)
            if (role.getPermissions() != null) {
                role.getPermissions().forEach(permission -> 
                    authorities.add(new SimpleGrantedAuthority(permission.getName().toUpperCase()))
                );
            }
            
            // BYPASS PARA EL ADMIN: Si es administrador, darle permisos por defecto
            // Esto soluciona el problema de que la tabla 'role_permissions' esté vacía en la BD.
            if ("ADMIN".equals(roleName) || "ADMINISTRADOR".equals(roleName)) {
                authorities.add(new SimpleGrantedAuthority("ROL_LEER"));
                authorities.add(new SimpleGrantedAuthority("ROL_CREAR"));
                authorities.add(new SimpleGrantedAuthority("ROL_ACTUALIZAR"));
                authorities.add(new SimpleGrantedAuthority("ROL_ELIMINAR"));
                
                authorities.add(new SimpleGrantedAuthority("PERMISO_LEER"));
                authorities.add(new SimpleGrantedAuthority("PERMISO_CREAR"));
                authorities.add(new SimpleGrantedAuthority("PERMISO_ACTUALIZAR"));
                authorities.add(new SimpleGrantedAuthority("PERMISO_ELIMINAR"));
                
                // Puedes agregar más permisos comodín aquí si el frontend los requiere
            }
        });

        // Retornar objeto User de Spring Security
        return new User(
                usuario.getUsername(),
                usuario.getPassword(),
                usuario.getEstado() != null ? usuario.getEstado() : true,
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                authorities
        );
    }
}
