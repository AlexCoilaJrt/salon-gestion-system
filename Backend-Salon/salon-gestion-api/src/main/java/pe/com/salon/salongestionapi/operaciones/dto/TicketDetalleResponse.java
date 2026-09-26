package pe.com.salon.salongestionapi.operaciones.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TicketDetalleResponse {
    private Long id;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;

    private Long servicioId;
    private String servicioNombre;

    private Long productoId;
    private String productoNombre;

    private Long empleadoId;
    private String empleadoNombreCompleto;
}
