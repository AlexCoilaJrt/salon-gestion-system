package pe.com.salon.salongestionapi.finanzas.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LiquidacionRequest {

    @NotNull(message = "El empleado es obligatorio")
    private Long empleadoId;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate fechaFin;
}
