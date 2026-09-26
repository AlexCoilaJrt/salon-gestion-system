package pe.com.salon.salongestionapi.rrhh.dto;

import lombok.Data;

@Data
public class EmpleadoResponse {
    private Long id;
    private String nombres;
    private String apellidos;
    private String dni;
    private String email;
    private String telefono;
    private java.time.LocalDate fechaNacimiento;
    private Integer edad;
    private String disponibilidad;
    private String especialidadNombre;
}
