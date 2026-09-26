package pe.com.salon.salongestionapi.analitica.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DesglosIngresosDTO {
    private BigDecimal ingresosPorServicios;
    private BigDecimal ingresosPorProductos;
    private BigDecimal totalGeneral;
    private String periodo;
}
