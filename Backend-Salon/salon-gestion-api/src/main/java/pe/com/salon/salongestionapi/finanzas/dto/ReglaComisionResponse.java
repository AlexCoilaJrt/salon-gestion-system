package pe.com.salon.salongestionapi.finanzas.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReglaComisionResponse {
    private Long id;
    private String nombre;
    private BigDecimal porcentaje;
    private Long especialidadId;
    private String especialidadNombre;
    private Boolean estado;
}
