package pe.com.salon.salongestionapi.operaciones.dto.impresion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImpresoraConfigDTO {
    private String ip;
    private Integer puerto;
    private Boolean autoImprimirAlCobrar;
    private Boolean autoCorte;
    private Boolean abrirGaveta;
    private String mensajePie;
    private Integer anchoPapelMm;
}

