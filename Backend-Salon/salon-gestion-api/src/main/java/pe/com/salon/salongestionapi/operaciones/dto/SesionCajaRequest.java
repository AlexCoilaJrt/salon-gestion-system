package pe.com.salon.salongestionapi.operaciones.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SesionCajaRequest {

    @NotNull(message = "El monto inicial es obligatorio")
    @Min(value = 0, message = "El monto inicial no puede ser negativo")
    private BigDecimal montoInicial;

}
