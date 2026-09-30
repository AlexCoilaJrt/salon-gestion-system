package pe.com.salon.salongestionapi.fidelizacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteCartillaDTO {
    private Long id;
    private Long clienteId;
    private String clienteNombre;
    private Long cartillaId;
    private String cartillaNombre;
    private String servicioRequerido;
    private Integer metaSellos;
    private Integer sellosActuales;
    private Double descuentoPremio;
    private Boolean completada;
    private Boolean canjeada;
    private LocalDateTime fechaUltimaActualizacion;
}
