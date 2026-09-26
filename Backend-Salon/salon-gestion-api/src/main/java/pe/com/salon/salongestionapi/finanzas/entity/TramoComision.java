package pe.com.salon.salongestionapi.finanzas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.salon.salongestionapi.rrhh.entity.Especialidad;

import java.math.BigDecimal;

/**
 * Tramo de comisión escalonada (Tiered Commission).
 *
 * Ejemplo de configuración para una Especialidad:
 *   Tramo 1: S/ 0.00  - S/ 2,999.99  → 35%
 *   Tramo 2: S/ 3,000 - S/ 5,999.99  → 40%
 *   Tramo 3: S/ 6,000 - ∞ (null)     → 45%
 *
 * Si especialidad = null, aplica globalmente a todas las especialidades.
 * El motor busca cuánto ha vendido el empleado en el mes y aplica el tramo que corresponde.
 */
@Entity
@Table(name = "tramo_comision")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TramoComision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "desde_monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal desdeMonto;

    // null = sin límite superior (tramo más alto)
    @Column(name = "hasta_monto", precision = 10, scale = 2)
    private BigDecimal hastaMonto;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;

    // null = aplica a todos independientemente de su especialidad
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especialidad_id")
    private Especialidad especialidad;

    @Column(nullable = false)
    private Boolean activo = true;
}
