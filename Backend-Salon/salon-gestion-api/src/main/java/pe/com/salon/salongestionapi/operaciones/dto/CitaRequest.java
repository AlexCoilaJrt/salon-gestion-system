package pe.com.salon.salongestionapi.operaciones.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import pe.com.salon.salongestionapi.operaciones.entity.EstadoCita;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CitaRequest {

    @NotNull(message = "La fecha y hora son obligatorias")
    @Future(message = "La cita debe ser programada para el futuro")
    private LocalDateTime fechaHora;

    private EstadoCita estado = EstadoCita.PENDIENTE;

    @Min(value = 0, message = "El adelanto no puede ser negativo")
    private BigDecimal adelanto;

    private String notas;

    @NotNull(message = "El cliente es obligatorio")
    private Long clienteId;

    @NotNull(message = "El empleado es obligatorio")
    private Long empleadoId;

    @NotNull(message = "El servicio es obligatorio")
    private Long servicioId;
}
