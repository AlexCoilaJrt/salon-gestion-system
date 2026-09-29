package pe.com.salon.salongestionapi.catalogo.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductoResponse {
    private Long id;
    private String nombre;
    private String marca;
    private String sku;
    private String proveedor;
    private String imageUrl;
    private BigDecimal precioVenta;
    private Integer stockActual;
    private Integer stockMinimo;
    private Boolean estado;
    private BigDecimal costo;
    private Boolean usoInterno;
    private Boolean ventaDirecta;
    private Long categoriaId;
    private String categoriaNombre;
}
