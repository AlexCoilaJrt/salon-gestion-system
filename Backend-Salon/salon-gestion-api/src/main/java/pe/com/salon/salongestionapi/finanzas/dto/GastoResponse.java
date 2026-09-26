package pe.com.salon.salongestionapi.finanzas.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GastoResponse {
    private Long id;
    private BigDecimal monto;
    private String descripcion;
    private LocalDateTime fechaGasto;
    private Boolean estado;
}
