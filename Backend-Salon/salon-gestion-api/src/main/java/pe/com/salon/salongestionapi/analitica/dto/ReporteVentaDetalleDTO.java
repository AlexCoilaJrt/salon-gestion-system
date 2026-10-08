package pe.com.salon.salongestionapi.analitica.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteVentaDetalleDTO {
    private Long ticketId;
    private LocalDateTime fechaHora;
    private String clienteNombre;
    private String empleadoNombre;
    private String tipoItem; // "Servicio", "Producto Retail", "Insumo"
    private String itemNombre;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private String estadoTicket; // "EMITIDO", "ANULADO"
}
