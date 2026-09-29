package pe.com.salon.salongestionapi.operaciones.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ResumenCajaResponse {
    private BigDecimal montoInicial;
    private BigDecimal totalVentasEfectivo;
    private BigDecimal totalVentasTransferencia;
    private BigDecimal totalEgresosEfectivo;
    private BigDecimal totalEsperadoEfectivo;
    private int cantidadTickets;
}
