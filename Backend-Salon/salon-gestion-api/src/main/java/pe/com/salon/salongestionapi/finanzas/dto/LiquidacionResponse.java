package pe.com.salon.salongestionapi.finanzas.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LiquidacionResponse {
    private Long id;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private BigDecimal totalVentas;
    private BigDecimal totalComisiones;
    private Long empleadoId;
    private String empleadoNombreCompleto;
    private Boolean pagado;
}
