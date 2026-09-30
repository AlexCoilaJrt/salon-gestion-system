package pe.com.salon.salongestionapi.operaciones.dto;

import lombok.Data;
import pe.com.salon.salongestionapi.operaciones.entity.EstadoCita;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CitaResponse {
    private Long id;
    private LocalDateTime fechaHora;
    private EstadoCita estado;
    private BigDecimal adelanto;
    private String metodoPago;
    private String notas;

    private Long clienteId;
    private String clienteNombreCompleto;

    private Long empleadoId;
    private String empleadoNombreCompleto;

    private Long servicioId;
    private String servicioNombre;

    private List<Long> productosIds;
}
