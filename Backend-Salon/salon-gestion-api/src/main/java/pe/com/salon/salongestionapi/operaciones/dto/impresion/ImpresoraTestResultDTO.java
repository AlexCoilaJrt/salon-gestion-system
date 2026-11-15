package pe.com.salon.salongestionapi.operaciones.dto.impresion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImpresoraTestResultDTO {
    private boolean conectado;
    private String mensaje;
    private Long latenciaMs;
    private String ip;
    private Integer puerto;
}

