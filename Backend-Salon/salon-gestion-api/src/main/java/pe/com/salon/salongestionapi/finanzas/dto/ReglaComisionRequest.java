package pe.com.salon.salongestionapi.finanzas.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReglaComisionRequest {

    @NotBlank(message = "El nombre de la regla es obligatorio")
    private String nombre;

    @NotNull(message = "El porcentaje es obligatorio")
    @DecimalMin(value = "0.0", message = "El porcentaje no puede ser negativo")
    @DecimalMax(value = "100.0", message = "El porcentaje no puede ser mayor a 100")
    private BigDecimal porcentaje;

    private Long especialidadId; // Opcional, si es null aplica a todos o por defecto
}
