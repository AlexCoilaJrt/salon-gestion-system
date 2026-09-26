package pe.com.salon.salongestionapi.rrhh.dto;

import lombok.Data;

@Data
public class EspecialidadResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean estado;
}
