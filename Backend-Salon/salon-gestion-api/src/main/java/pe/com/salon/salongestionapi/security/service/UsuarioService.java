package pe.com.salon.salongestionapi.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.com.salon.salongestionapi.security.dto.UsuarioResponseDTO;
import pe.com.salon.salongestionapi.security.dto.UsuarioRequestDTO;
import pe.com.salon.salongestionapi.security.repository.UsuarioRepository;
import pe.com.salon.salongestionapi.security.entity.Usuario;
import pe.com.salon.salongestionapi.roles.repository.RoleRepository;
import pe.com.salon.salongestionapi.roles.entity.Role;
import pe.com.salon.salongestionapi.shared.PageResponse;
import pe.com.salon.salongestionapi.exception.ResourceNotFoundException;
import pe.com.salon.salongestionapi.exception.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponseDTO> getAllUsuarios(Pageable pageable) {
        Page<pe.com.salon.salongestionapi.security.entity.Usuario> page = usuarioRepository.findAll(pageable);
        
        List<UsuarioResponseDTO> content = page.getContent().stream().map(this::mapToDTO).collect(Collectors.toList());

        return PageResponse.<UsuarioResponseDTO>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .build();
    }

    @Transactional
    public UsuarioResponseDTO createUsuario(UsuarioRequestDTO request) {
        if (usuarioRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BadRequestException("El nombre de usuario ya está en uso");
        }
        
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new BadRequestException("La contraseña es obligatoria para un usuario nuevo");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(request.getUsername());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setEmail(request.getEmail());
        usuario.setEstado(request.getEstado() != null ? request.getEstado() : true);
        
        assignRoles(usuario, request.getRoleIds());
        
        return mapToDTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponseDTO updateUsuario(Long id, UsuarioRequestDTO request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        // Verificar si el username cambió y si el nuevo ya existe
        if (!usuario.getUsername().equals(request.getUsername()) && 
            usuarioRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BadRequestException("El nombre de usuario ya está en uso");
        }

        usuario.setUsername(request.getUsername());
        usuario.setEmail(request.getEmail());
        if (request.getEstado() != null) {
            usuario.setEstado(request.getEstado());
        }

        // Si mandan password, actualizarlo
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        assignRoles(usuario, request.getRoleIds());

        return mapToDTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public void toggleUsuarioStatus(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        
        // Evitar que el admin principal se bloquee a sí mismo
        if ("admin@salon.com".equalsIgnoreCase(usuario.getUsername())) {
             throw new BadRequestException("No puedes bloquear al administrador principal");
        }
        
        usuario.setEstado(!usuario.getEstado());
        usuarioRepository.save(usuario);
    }

    private void assignRoles(Usuario usuario, Set<Long> roleIds) {
        if (roleIds != null && !roleIds.isEmpty()) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(roleIds));
            if (roles.isEmpty()) {
                throw new BadRequestException("Los roles seleccionados no son válidos");
            }
            usuario.setRoles(roles);
        } else {
            throw new BadRequestException("Debe asignar al menos un rol al usuario");
        }
    }

    private UsuarioResponseDTO mapToDTO(Usuario usuario) {
        String nombre = "Sin asignar";
        String apellido = "";
        
        if (usuario.getEmpleado() != null) {
            nombre = usuario.getEmpleado().getNombres();
            apellido = usuario.getEmpleado().getApellidos();
        } else {
            // Intenta extraer un nombre amigable del email como fallback temporal
            if (usuario.getEmail() != null && usuario.getEmail().contains("@")) {
                nombre = usuario.getEmail().split("@")[0].toUpperCase();
                apellido = "(Sistema)";
            }
        }
        
        String rol = "SIN ROL";
        if (usuario.getRoles() != null && !usuario.getRoles().isEmpty()) {
            rol = usuario.getRoles().iterator().next().getName();
        }

        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .email(usuario.getEmail())
                .nombre(nombre)
                .apellido(apellido)
                .rol(rol)
                .estado(usuario.getEstado())
                .ultimoAcceso(usuario.getLastLogin())
                .build();
    }
}
