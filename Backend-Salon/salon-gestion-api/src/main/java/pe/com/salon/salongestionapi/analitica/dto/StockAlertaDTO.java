package pe.com.salon.salongestionapi.analitica.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockAlertaDTO {
    private Long productoId;
    private String nombre;
    private String marca;
    private Integer stockActual;
    private Integer stockMinimo;
    private Integer unidadesFaltantes;
}
