package pe.com.salon.salongestionapi.security.controller;

import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.com.salon.salongestionapi.security.dto.UsuarioResponseDTO;
import pe.com.salon.salongestionapi.security.dto.UsuarioRequestDTO;
import pe.com.salon.salongestionapi.security.service.UsuarioService;
import pe.com.salon.salongestionapi.shared.ApiResponse;
import pe.com.salon.salongestionapi.shared.PageResponse;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROL_LEER')") 
    public ResponseEntity<ApiResponse<PageResponse<UsuarioResponseDTO>>> getAllUsuarios(
            @PageableDefault(page = 0, size = 10, sort = "username") Pageable pageable) {

        PageResponse<UsuarioResponseDTO> response = usuarioService.getAllUsuarios(pageable);

        return ResponseEntity.ok(ApiResponse.<PageResponse<UsuarioResponseDTO>>builder()
                .success(true)
                .message("Usuarios obtenidos exitosamente")
                .data(response)
                .build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROL_CREAR')")
    public ResponseEntity<ApiResponse<UsuarioResponseDTO>> createUsuario(
            @Valid @RequestBody UsuarioRequestDTO request) {
        
        UsuarioResponseDTO response = usuarioService.createUsuario(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<UsuarioResponseDTO>builder()
                .success(true)
                .message("Usuario creado exitosamente")
                .data(response)
                .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROL_ACTUALIZAR')")
    public ResponseEntity<ApiResponse<UsuarioResponseDTO>> updateUsuario(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioRequestDTO request) {
            
        UsuarioResponseDTO response = usuarioService.updateUsuario(id, request);
        
        return ResponseEntity.ok(ApiResponse.<UsuarioResponseDTO>builder()
                .success(true)
                .message("Usuario actualizado exitosamente")
                .data(response)
                .build());
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasAuthority('ROL_ELIMINAR')") // O un permiso específico para bloquear
    public ResponseEntity<ApiResponse<Void>> toggleUsuarioStatus(@PathVariable Long id) {
        
        usuarioService.toggleUsuarioStatus(id);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Estado del usuario modificado exitosamente")
                .build());
    }
}
