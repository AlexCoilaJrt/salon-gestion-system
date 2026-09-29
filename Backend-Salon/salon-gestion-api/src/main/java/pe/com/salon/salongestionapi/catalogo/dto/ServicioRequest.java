package pe.com.salon.salongestionapi.catalogo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ServicioRequest {

    @NotBlank(message = "El nombre del servicio es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    private String descripcion;

    @NotNull(message = "El precio base es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    private BigDecimal precioBase;

    @NotNull(message = "La duración en minutos es obligatoria")
    @Min(value = 1, message = "La duración debe ser al menos de 1 minuto")
    private Integer duracionMinutos;

    @DecimalMin(value = "0.0", message = "La comisión debe ser al menos 0")
    @DecimalMax(value = "100.0", message = "La comisión no puede exceder 100")
    private BigDecimal comisionPorcentaje;

    @DecimalMin(value = "0.0", message = "El costo de material debe ser al menos 0")
    private BigDecimal costoMaterial;

    @NotNull(message = "Debe asignar una especialidad requerida para este servicio")
    private Long especialidadRequeridaId;

    @NotNull(message = "Debe asignar una categoría a este servicio")
    private Long categoriaId;

    private List<Long> insumosIds;
}
