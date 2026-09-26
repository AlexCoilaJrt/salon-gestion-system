package pe.com.salon.salongestionapi.finanzas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.salon.salongestionapi.rrhh.entity.Empleado;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "liquidacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Liquidacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "total_ventas", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalVentas = BigDecimal.ZERO;

    @Column(name = "total_comisiones", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalComisiones = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Column(nullable = false)
    private Boolean pagado = false;
}
