package pe.com.salon.salongestionapi.rrhh.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import pe.com.salon.salongestionapi.rrhh.entity.EstadoLiquidacion;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LiquidacionRequest {

    @NotNull(message = "El ID del empleado es requerido")
    private Long empleadoId;

    @NotNull(message = "La fecha de inicio es requerida")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es requerida")
    private LocalDate fechaFin;

    private BigDecimal totalVentas = BigDecimal.ZERO;
    private BigDecimal totalComision = BigDecimal.ZERO;
    private BigDecimal sueldoFijoProporcional = BigDecimal.ZERO;
    private BigDecimal descuentos = BigDecimal.ZERO;

    @NotNull(message = "El total a pagar es requerido")
    private BigDecimal totalAPagar;

    @NotNull(message = "El estado de liquidación es requerido")
    private EstadoLiquidacion estado;

    private Integer diasAsistidos = 0;

    private BigDecimal montoRetenido = BigDecimal.ZERO;
    private BigDecimal montoLiberado = BigDecimal.ZERO;
    private BigDecimal penalidadAbandono = BigDecimal.ZERO;
    private Boolean esCese = false;
}
