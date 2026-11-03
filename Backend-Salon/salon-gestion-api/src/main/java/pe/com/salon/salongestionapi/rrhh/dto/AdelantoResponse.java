package pe.com.salon.salongestionapi.rrhh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdelantoResponse {
    private Long id;
    private Long empleadoId;
    private String empleadoNombreCompleto;
    private BigDecimal monto;
    private LocalDate fecha;
    private String metodoPago;
    private String motivo;
    private Long egresoId;
    private String usuarioRegistro;
    private LocalDateTime fechaRegistro;
    private String estado;
}

