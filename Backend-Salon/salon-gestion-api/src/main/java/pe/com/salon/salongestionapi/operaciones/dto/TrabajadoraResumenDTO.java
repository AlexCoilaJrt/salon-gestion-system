package pe.com.salon.salongestionapi.operaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrabajadoraResumenDTO {
    private Long empleadoId;
    private String nombreCompleto;
    private long cantidadServicios;
    private BigDecimal totalGenerado;
    private List<ServicioConteoDTO> serviciosDetalle;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServicioConteoDTO {
        private String servicioNombre;
        private int cantidad;
        private BigDecimal subtotal;
    }
}
