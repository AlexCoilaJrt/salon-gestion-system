package pe.com.salon.salongestionapi.operaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientosCajaResponse {
    private String periodo;
    private String fechaFiltro;
    private BigDecimal totalVentas;
    private BigDecimal totalEfectivo;
    private BigDecimal totalTransferencia;
    private int cantidadTickets;
    private List<TicketResponse> tickets;
    private List<TrabajadoraResumenDTO> rankingTrabajadoras;
}
