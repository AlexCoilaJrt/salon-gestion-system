package pe.com.salon.salongestionapi.rrhh.dto;

import lombok.Data;
import pe.com.salon.salongestionapi.rrhh.entity.TipoComision;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ComisionResponse {
    private Long id;
    private Long empleadoId;
    private String empleadoNombre;
    private String empleadoApellido;
    private TipoComision tipoComision;
    private BigDecimal valor;
    private Boolean estado;
    private LocalDateTime fechaRegistro;
}
