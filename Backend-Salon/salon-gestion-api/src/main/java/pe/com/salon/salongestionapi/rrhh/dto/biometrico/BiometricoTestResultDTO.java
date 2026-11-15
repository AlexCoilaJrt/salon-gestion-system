package pe.com.salon.salongestionapi.rrhh.dto.biometrico;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometricoTestResultDTO {
    private boolean conectado;
    private String ip;
    private int puerto;
    private Long latenciaMs;
    private String mensaje;
    private String detallesTecnicos;
}
