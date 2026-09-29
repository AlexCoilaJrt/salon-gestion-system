package pe.com.salon.salongestionapi.rrhh.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import pe.com.salon.salongestionapi.rrhh.entity.TipoComision;
import java.math.BigDecimal;

@Data
public class ComisionRequest {

    @NotNull(message = "El ID del empleado es requerido")
    private Long empleadoId;

    @NotNull(message = "El tipo de comisión es requerido")
    private TipoComision tipoComision;

    @NotNull(message = "El valor de la comisión es requerido")
    @DecimalMin(value = "0.0", inclusive = false, message = "El valor debe ser mayor a 0")
    private BigDecimal valor;

    private Boolean estado;
}
