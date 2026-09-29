package pe.com.salon.salongestionapi.rrhh.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class EmpleadoRequest {
    
    @NotBlank(message = "Los nombres son obligatorios")
    @Size(min = 2, max = 60, message = "Los nombres deben tener entre 2 y 60 caracteres")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(min = 2, max = 60, message = "Los apellidos deben tener entre 2 y 60 caracteres")
    private String apellidos;

    @NotBlank(message = "El DNI/Documento es obligatorio")
    @Pattern(regexp = "^[0-9]{8,15}$", message = "El documento debe contener solo números (entre 8 y 15 dígitos)")
    private String dni;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Debe ser un correo electrónico válido")
    @Size(max = 100, message = "El email no puede tener más de 100 caracteres")
    private String email;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{9,15}$", message = "El teléfono debe contener solo números (entre 9 y 15 dígitos)")
    private String telefono;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    private java.time.LocalDate fechaNacimiento;

    @NotEmpty(message = "Debe asignar al menos una especialidad al empleado")
    private java.util.List<Long> especialidadIds;

    @NotNull(message = "Debe asignar un turno al empleado")
    private Long turnoId;

    private Boolean estado;
}
