package pe.com.salon.salongestionapi.fidelizacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartillaDTO {
    private Long id;
    private String nombre;
    private Long servicioId;
    private String servicioNombre;
    private Integer metaSellos;
    private Double descuentoPremio;
    private Boolean estado;
}
