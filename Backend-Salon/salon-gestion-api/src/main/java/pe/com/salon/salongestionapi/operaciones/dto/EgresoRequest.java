package pe.com.salon.salongestionapi.operaciones.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class EgresoRequest {
    private BigDecimal monto;
    private String categoria;
    private String motivo;
}
