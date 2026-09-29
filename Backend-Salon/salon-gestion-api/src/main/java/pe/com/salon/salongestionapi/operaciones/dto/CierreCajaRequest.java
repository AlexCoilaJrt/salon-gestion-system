package pe.com.salon.salongestionapi.operaciones.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CierreCajaRequest {

    @NotNull(message = "El monto declarado es obligatorio")
    @Min(value = 0, message = "El monto declarado no puede ser negativo")
    private BigDecimal montoDeclarado;

    private String observaciones;
}
