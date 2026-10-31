package pe.com.salon.salongestionapi.operaciones.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class ActualizarPreciosTicketRequest {

    @NotEmpty(message = "Debe enviar al menos un detalle para actualizar")
    private List<DetallePrecioItem> detalles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetallePrecioItem {
        @NotNull(message = "El id del detalle es obligatorio")
        private Long id;

        @NotNull(message = "El precio unitario es obligatorio")
        private BigDecimal precioUnitario;

        private Integer cantidad;
    }
}
