package pe.com.salon.salongestionapi.rrhh.dto;

import lombok.Data;

@Data
public class EspecialidadResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private String tipoPago;
    private java.math.BigDecimal montoFijo;
    private java.math.BigDecimal porcentajeComision;
    private Boolean estado;
}
