package pe.com.salon.salongestionapi.empresa.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmpresaRequest(
    @NotBlank(message = "El nombre comercial es obligatorio")
    @Size(max = 150)
    String nombreComercial,

    @NotBlank(message = "La razón social es obligatoria")
    @Size(max = 150)
    String razonSocial,

    @Size(max = 20)
    String ruc,

    @Size(max = 255)
    String direccion,

    @Size(max = 50)
    String telefono,

    @Email(message = "Debe ser un correo válido")
    @Size(max = 100)
    String email,

    String logoUrl
) {
}
