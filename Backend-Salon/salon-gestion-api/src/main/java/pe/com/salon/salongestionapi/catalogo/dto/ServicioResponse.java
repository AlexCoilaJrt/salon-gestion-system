package pe.com.salon.salongestionapi.catalogo.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ServicioResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal precioBase;
    private Integer duracionMinutos;
    private Boolean estado;
    private String especialidadRequeridaNombre;
}
