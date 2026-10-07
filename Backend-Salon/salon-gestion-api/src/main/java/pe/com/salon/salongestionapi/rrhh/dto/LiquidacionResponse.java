package pe.com.salon.salongestionapi.rrhh.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Data
public class LiquidacionResponse {
    private Long id;
    private Long empleadoId;
    private String empleadoNombreCompleto;
    
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Integer diasAsistidos;
    
    private BigDecimal sueldoFijoProporcional;
    private BigDecimal totalVentas;
    private BigDecimal totalComision;
    private BigDecimal descuentosAdelantos;
    private BigDecimal totalPagar;
    
    private BigDecimal montoRetenido;
    private BigDecimal montoLiberado;
    private BigDecimal penalidadAbandono;
    private Boolean esCese;
    
    private String estado;
    private LocalDateTime fechaRegistro;
}
