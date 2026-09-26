package pe.com.salon.salongestionapi.finanzas.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TramoComisionRequest {

    @NotNull(message = "El monto desde es obligatorio")
    @DecimalMin(value = "0.0", message = "El monto desde no puede ser negativo")
    private BigDecimal desdeMonto;

    // null = sin límite superior
    private BigDecimal hastaMonto;

    @NotNull(message = "El porcentaje es obligatorio")
    @DecimalMin(value = "0.0", message = "El porcentaje no puede ser negativo")
    @DecimalMax(value = "100.0", message = "El porcentaje no puede superar 100")
    private BigDecimal porcentaje;

    // null = aplica globalmente a todos
    private Long especialidadId;
}
