package pe.com.salon.salongestionapi.rrhh.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalTime;

@Data
@Builder
public class KioskoResponse {
    private String mensaje;
    private String empleadoNombre;
    private String tipoRegistro; // "ENTRADA" o "SALIDA"
    private LocalTime horaRegistro;
}
