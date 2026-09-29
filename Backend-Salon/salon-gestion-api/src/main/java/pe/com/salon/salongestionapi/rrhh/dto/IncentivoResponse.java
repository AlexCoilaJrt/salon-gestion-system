package pe.com.salon.salongestionapi.rrhh.dto;

import lombok.Data;
import pe.com.salon.salongestionapi.rrhh.entity.TipoComision;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class IncentivoResponse {
    private Long id;
    private String nombre;
    private TipoComision tipoIncentivo;
    private BigDecimal valor;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Boolean estado;
    private LocalDateTime fechaRegistro;
}
