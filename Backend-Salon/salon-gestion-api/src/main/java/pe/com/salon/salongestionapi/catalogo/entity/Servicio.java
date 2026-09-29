package pe.com.salon.salongestionapi.catalogo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.salon.salongestionapi.rrhh.entity.Especialidad;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "servicio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(name = "precio_base", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioBase;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    // GAP 3: Comisión propia del servicio (override sobre la de Especialidad)
    // Null = usar la regla de la especialidad o el default
    @Column(name = "comision_porcentaje", precision = 5, scale = 2)
    private BigDecimal comisionPorcentaje;

    // GAP 5: Costo del material utilizado en el servicio
    // Se descuenta antes de calcular la comisión: base = precioBase - costoMaterial
    @Column(name = "costo_material", precision = 10, scale = 2)
    private BigDecimal costoMaterial = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean estado = true;

    // Relación con RRHH: Un servicio requiere de un especialista en particular
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidadRequerida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ManyToMany
    @JoinTable(
        name = "servicio_insumos",
        joinColumns = @JoinColumn(name = "servicio_id"),
        inverseJoinColumns = @JoinColumn(name = "producto_id")
    )
    private List<Producto> insumos = new ArrayList<>();
}
