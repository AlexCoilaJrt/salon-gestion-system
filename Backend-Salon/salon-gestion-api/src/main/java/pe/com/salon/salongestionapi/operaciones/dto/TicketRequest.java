package pe.com.salon.salongestionapi.operaciones.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import pe.com.salon.salongestionapi.operaciones.entity.MetodoPago;

import java.util.List;

@Data
public class TicketRequest {

    @NotNull(message = "El método de pago es obligatorio")
    private MetodoPago metodoPago;

    private Long clienteId;

    private String nombreClienteNoRegistrado;

    @NotEmpty(message = "El ticket debe tener al menos un detalle (servicio o producto)")
    @Valid
    private List<TicketDetalleRequest> detalles;

    // Optional: Service ID selected by the client to apply the loyalty stamp to
    private Long servicioSelloId;

    // Optional: List of Cartilla IDs that are being redeemed in this ticket
    private List<Long> premiosFidelizacionIds;
}
