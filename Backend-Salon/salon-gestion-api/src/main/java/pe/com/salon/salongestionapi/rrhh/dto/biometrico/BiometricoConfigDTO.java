package pe.com.salon.salongestionapi.rrhh.dto.biometrico;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometricoConfigDTO {
    private String ip;
    private Integer puerto;
    private String commKey;
    private String nombreDispositivo;
    private Boolean autoSincronizar;
    private Integer intervaloMinutos;
    private Boolean activo;
}
