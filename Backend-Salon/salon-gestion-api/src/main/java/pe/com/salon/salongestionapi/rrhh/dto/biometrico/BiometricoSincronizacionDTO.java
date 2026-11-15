package pe.com.salon.salongestionapi.rrhh.dto.biometrico;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometricoSincronizacionDTO {
    private boolean exito;
    private int totalMarcacionesLeidas;
    private int nuevasAsistenciasRegistradas;
    private LocalDateTime fechaSincronizacion;
    private String mensaje;
}
