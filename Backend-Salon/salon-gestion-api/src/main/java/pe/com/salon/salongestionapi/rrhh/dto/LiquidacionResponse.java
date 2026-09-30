package pe.com.salon.salongestionapi.rrhh.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class LiquidacionResponse {
    private Long empleadoId;
    private String empleadoNombreCompleto;
    private BigDecimal sueldoFijo;
    private BigDecimal totalVentas;
    private BigDecimal totalComision;
    private BigDecimal descuentosAdelantos;
    private BigDecimal totalPagar;
}
