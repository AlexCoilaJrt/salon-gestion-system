package pe.com.salon.salongestionapi.rrhh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EspecialidadRequest {

    @NotBlank(message = "El nombre de la especialidad es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String nombre;

    @Size(max = 200, message = "La descripción no puede superar los 200 caracteres")
    private String descripcion;

    private String tipoPago;
    private java.math.BigDecimal montoFijo;
    private java.math.BigDecimal porcentajeComision;

    private Boolean estado;
}
