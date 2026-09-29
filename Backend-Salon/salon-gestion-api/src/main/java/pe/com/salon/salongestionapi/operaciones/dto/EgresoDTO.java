package pe.com.salon.salongestionapi.operaciones.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EgresoDTO {
    private Long id;
    private BigDecimal monto;
    private String categoria;
    private String motivo;
    private LocalDateTime fechaHora;
    private String usuarioNombre;
}
