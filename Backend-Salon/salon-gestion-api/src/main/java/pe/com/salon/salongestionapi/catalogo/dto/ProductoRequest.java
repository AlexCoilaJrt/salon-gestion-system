package pe.com.salon.salongestionapi.catalogo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductoRequest {

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @Size(max = 50, message = "La marca no puede superar los 50 caracteres")
    private String marca;

    private String sku;
    private String proveedor;
    private String imageUrl;

    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio debe ser 0 o mayor")
    private BigDecimal precioVenta;

    @NotNull(message = "El stock actual es obligatorio")
    @Min(value = 0, message = "El stock actual no puede ser negativo")
    private Integer stockActual;

    @NotNull(message = "El stock mínimo es obligatorio")
    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    private Integer stockMinimo;

    @NotNull(message = "El costo es obligatorio")
    @DecimalMin(value = "0.0", message = "El costo no puede ser negativo")
    private BigDecimal costo;

    @NotNull(message = "Debe indicar si es para uso interno")
    private Boolean usoInterno;

    @NotNull(message = "Debe indicar si es para venta directa")
    private Boolean ventaDirecta;

    @NotNull(message = "Debe asignar una categoría a este producto")
    private Long categoriaId;
}
