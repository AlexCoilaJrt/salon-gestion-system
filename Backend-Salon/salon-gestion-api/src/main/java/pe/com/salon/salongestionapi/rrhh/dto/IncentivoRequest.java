package pe.com.salon.salongestionapi.rrhh.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import pe.com.salon.salongestionapi.rrhh.entity.TipoComision;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class IncentivoRequest {

    @NotBlank(message = "El nombre es requerido")
    private String nombre;

    @NotNull(message = "El tipo de incentivo es requerido")
    private TipoComision tipoIncentivo;

    @NotNull(message = "El valor es requerido")
    @DecimalMin(value = "0.0", inclusive = false, message = "El valor debe ser mayor a 0")
    private BigDecimal valor;

    @NotNull(message = "La fecha de inicio es requerida")
    private LocalDateTime fechaInicio;

    @NotNull(message = "La fecha de fin es requerida")
    private LocalDateTime fechaFin;

    private Boolean estado;
}
