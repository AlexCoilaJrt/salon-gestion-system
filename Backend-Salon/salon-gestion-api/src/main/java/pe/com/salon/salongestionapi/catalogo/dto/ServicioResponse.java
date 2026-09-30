package pe.com.salon.salongestionapi.catalogo.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ServicioResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal precioBase;
    private Integer duracionMinutos;
    private BigDecimal comisionPorcentaje;
    private BigDecimal costoMaterial;
    private Boolean estado;
    private Long especialidadRequeridaId;
    private String especialidadRequeridaNombre;
    private Long categoriaId;
    private String categoriaNombre;

    private List<ProductoResponse> insumos;

    private String imageUrl;
}
