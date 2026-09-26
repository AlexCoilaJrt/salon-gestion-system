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

    @NotNull(message = "El ID del cliente es obligatorio")
    private Long clienteId;

    @NotEmpty(message = "El ticket debe tener al menos un detalle (servicio o producto)")
    @Valid
    private List<TicketDetalleRequest> detalles;
}
