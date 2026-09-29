package pe.com.salon.salongestionapi.catalogo.dto;

import lombok.Data;

@Data
public class CategoriaResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean estado;
}
