package pe.com.salon.salongestionapi.rrhh.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import pe.com.salon.salongestionapi.rrhh.entity.TipoAsistencia;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AsistenciaRequest {

    @NotNull(message = "El empleado es obligatorio")
    private Long empleadoId;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    private LocalTime horaEntrada;

    private LocalTime horaSalida;

    private Integer tardanzaMinutos = 0;

    @NotNull(message = "El tipo de asistencia es obligatorio")
    private TipoAsistencia tipo;

    private String observaciones;
}
