package pe.com.salon.salongestionapi.operaciones.dto;

import lombok.Data;

@Data
public class ClienteResponse {
    private Long id;
    private String nombres;
    private String apellidos;
    private String telefono;
    private String email;
    private Boolean estado;
}
