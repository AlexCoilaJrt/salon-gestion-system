package pe.com.salon.salongestionapi.operaciones.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SesionCajaResponse {
    private Long id;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private BigDecimal montoInicial;
    private BigDecimal montoFinal;
    private Boolean estado;
}
