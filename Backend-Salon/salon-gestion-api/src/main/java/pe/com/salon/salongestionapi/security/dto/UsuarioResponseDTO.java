package pe.com.salon.salongestionapi.security.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UsuarioResponseDTO {
    private Long id;
    private String username;
    private String email;
    private String nombre;
    private String apellido;
    private String rol;
    private Boolean estado;
    private LocalDateTime ultimoAcceso;
}
