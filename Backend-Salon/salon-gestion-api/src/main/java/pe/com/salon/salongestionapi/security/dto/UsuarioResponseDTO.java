package pe.com.salon.salongestionapi.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
