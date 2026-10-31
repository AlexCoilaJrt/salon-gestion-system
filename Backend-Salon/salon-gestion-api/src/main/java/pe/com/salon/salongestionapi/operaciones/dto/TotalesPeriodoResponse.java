package pe.com.salon.salongestionapi.operaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotalesPeriodoResponse {
    private BigDecimal totalHoy;
    private long cantidadVentasHoy;

    private BigDecimal totalSemana;
    private long cantidadVentasSemana;

    private BigDecimal totalMes;
    private long cantidadVentasMes;
}
