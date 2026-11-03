package pe.com.salon.salongestionapi.rrhh.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AdelantoRequest {
    private Long empleadoId;
    private BigDecimal monto;
    private LocalDate fecha;
    private String metodoPago;
    private String motivo;
    private Boolean descontarDeCaja;
}

