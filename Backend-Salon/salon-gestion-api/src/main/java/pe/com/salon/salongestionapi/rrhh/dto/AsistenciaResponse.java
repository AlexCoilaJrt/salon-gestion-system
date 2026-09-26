package pe.com.salon.salongestionapi.rrhh.dto;

import lombok.Data;
import pe.com.salon.salongestionapi.rrhh.entity.TipoAsistencia;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AsistenciaResponse {
    private Long id;
    private LocalDate fecha;
    private LocalTime horaEntrada;
    private LocalTime horaSalida;
    private Integer tardanzaMinutos;
    private TipoAsistencia tipo;
    private String observaciones;
    private Long empleadoId;
    private String empleadoNombreCompleto;
}
