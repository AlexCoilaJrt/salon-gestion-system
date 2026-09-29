package pe.com.salon.salongestionapi.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import java.util.Set;

@Data
@Builder
public class UsuarioRequestDTO {

    @NotBlank(message = "El username es obligatorio")
    private String username;

    private String password; // Solo obligatorio en creación

    @Email(message = "Debe ser un correo electrónico válido")
    private String email;

    @NotNull(message = "Debe asignar al menos un rol")
    private Set<Long> roleIds;

    private Boolean estado;
    
    // Opcional, si deseas vincular un empleado
    private Long empleadoId;
}
