package pe.com.salon.salongestionapi.operaciones.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TicketDetalleRequest {

    private Long servicioId;
    
    private Long productoId;
    
    private Long empleadoId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima es 1")
    private Integer cantidad;
}
