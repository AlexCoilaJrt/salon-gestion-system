package pe.com.salon.salongestionapi.rrhh.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class MonitorComisionResponse {
    private Long empleadoId;
    private String empleadoNombreCompleto;
    private Boolean enTurno;
    private Boolean asistenciaActiva;
    private BigDecimal basePorcentaje;
    private BigDecimal baseMontoFijo;
    private List<IncentivoAplicado> incentivosAplicados;
    private BigDecimal totalComisionPorcentaje;
    private BigDecimal totalComisionMontoFijo;

    @Data
    public static class IncentivoAplicado {
        private String nombre;
        private String tipo;
        private BigDecimal valor;
    }
}
