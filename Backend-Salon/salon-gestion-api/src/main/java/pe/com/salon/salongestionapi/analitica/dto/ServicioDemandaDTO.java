package pe.com.salon.salongestionapi.analitica.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServicioDemandaDTO {
    private Long servicioId;
    private String servicioNombre;
    private Long cantidadVendida;
    private BigDecimal totalIngresado;
}
