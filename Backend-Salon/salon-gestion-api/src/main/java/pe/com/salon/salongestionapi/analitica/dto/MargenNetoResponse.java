package pe.com.salon.salongestionapi.analitica.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MargenNetoResponse {
    private BigDecimal ingresosTotales;
    private BigDecimal ingresosServicios;
    private BigDecimal ingresosProductos;
    private BigDecimal gastosOperativos;
    private BigDecimal pagoComisiones;
    private BigDecimal gananciaNeta;
    private String periodo;
}
