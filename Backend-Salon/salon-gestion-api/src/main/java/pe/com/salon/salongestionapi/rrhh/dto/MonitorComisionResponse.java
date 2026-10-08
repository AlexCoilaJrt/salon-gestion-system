package pe.com.salon.salongestionapi.rrhh.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;

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
    
    private BigDecimal ventasHoy;
    private BigDecimal comisionesGanadasHoy;
    
    private List<DetalleServicio> detallesServicios;

    @Data
    public static class DetalleServicio {
        private String servicioNombre;
        private String categoriaNombre;
        private String especialidadAplicada;
        private String tipoPago;
        private BigDecimal porcentajeAplicado;
        private BigDecimal precioCobrado;
        private BigDecimal comisionGanada;
        private LocalDateTime fechaHora;
    }

    @Data
    public static class IncentivoAplicado {
        private String nombre;
        private String tipo;
        private BigDecimal valor;
    }
}
