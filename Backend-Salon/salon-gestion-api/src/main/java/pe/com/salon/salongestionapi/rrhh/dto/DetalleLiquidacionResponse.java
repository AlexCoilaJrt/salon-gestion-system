package pe.com.salon.salongestionapi.rrhh.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Una línea del desglose de una liquidación: un servicio (o producto) que el especialista vendió
 * en el periodo, con la comisión que le generó.
 */
@Data
public class DetalleLiquidacionResponse {
    private Long ticketId;
    private LocalDateTime fecha;
    private String tipo; // SERVICIO | PRODUCTO
    private String descripcion;
    private String cliente;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private BigDecimal comision;
}
