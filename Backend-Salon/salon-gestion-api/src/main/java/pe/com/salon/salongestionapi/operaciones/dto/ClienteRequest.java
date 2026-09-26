package pe.com.salon.salongestionapi.operaciones.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClienteRequest {

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 60, message = "Los nombres no pueden superar los 60 caracteres")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 60, message = "Los apellidos no pueden superar los 60 caracteres")
    private String apellidos;

    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    private String telefono;

    @Email(message = "El formato de email no es válido")
    @Size(max = 100, message = "El email no puede superar los 100 caracteres")
    private String email;
}
