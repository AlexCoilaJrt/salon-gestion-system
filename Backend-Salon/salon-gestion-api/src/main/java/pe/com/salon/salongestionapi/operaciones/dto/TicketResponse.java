package pe.com.salon.salongestionapi.operaciones.dto;

import lombok.Data;
import pe.com.salon.salongestionapi.operaciones.entity.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TicketResponse {
    private Long id;
    private LocalDateTime fechaEmision;
    private MetodoPago metodoPago;
    private BigDecimal total;
    private Long clienteId;
    private String clienteNombreCompleto;
    private Long sesionCajaId;
    private Boolean activo;
    private List<TicketDetalleResponse> detalles;
}
