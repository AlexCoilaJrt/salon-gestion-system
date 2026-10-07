package pe.com.salon.salongestionapi.rrhh.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity(name = "LiquidacionRrhh")
@Table(name = "liquidaciones_rrhh")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Liquidacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "total_ventas")
    @Builder.Default
    private BigDecimal totalVentas = BigDecimal.ZERO;

    @Column(name = "total_comision")
    @Builder.Default
    private BigDecimal totalComision = BigDecimal.ZERO;

    @Column(name = "sueldo_fijo_proporcional")
    @Builder.Default
    private BigDecimal sueldoFijoProporcional = BigDecimal.ZERO;

    @Column(name = "descuentos")
    @Builder.Default
    private BigDecimal descuentos = BigDecimal.ZERO;

    @Column(name = "total_a_pagar", nullable = false)
    @Builder.Default
    private BigDecimal totalAPagar = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoLiquidacion estado = EstadoLiquidacion.PAGADO;

    @Column(name = "dias_asistidos")
    private Integer diasAsistidos;

    @Column(name = "monto_retenido")
    @Builder.Default
    private BigDecimal montoRetenido = BigDecimal.ZERO;

    @Column(name = "monto_liberado")
    @Builder.Default
    private BigDecimal montoLiberado = BigDecimal.ZERO;

    @Column(name = "penalidad_abandono")
    @Builder.Default
    private BigDecimal penalidadAbandono = BigDecimal.ZERO;

    @Column(name = "es_cese")
    @Builder.Default
    private Boolean esCese = false;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @PrePersist
    public void prePersist() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }
}
