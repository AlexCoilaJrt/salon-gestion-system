package pe.com.salon.salongestionapi.finanzas.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TramoComisionResponse {
    private Long id;
    private BigDecimal desdeMonto;
    private BigDecimal hastaMonto;
    private BigDecimal porcentaje;
    private Long especialidadId;
    private String especialidadNombre;
    private Boolean activo;
}
