package pe.com.salon.salongestionapi.analitica.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RankingEspecialistaDTO {
    private Long empleadoId;
    private String nombreCompleto;
    private Long cantidadServicios;
    private BigDecimal totalVendido;
}
