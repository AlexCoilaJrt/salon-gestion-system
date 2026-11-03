package pe.com.salon.salongestionapi.rrhh.entity;

import jakarta.persistence.*;
import lombok.*;
import pe.com.salon.salongestionapi.rrhh.entity.Empleado;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "adelantos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Adelanto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "metodo_pago", nullable = false, length = 50)
    private String metodoPago; // "EFECTIVO", "YAPE", "PLIN", "TRANSFERENCIA"

    @Column(nullable = false, length = 300)
    private String motivo;

    @Column(name = "egreso_id")
    private Long egresoId;

    @Column(name = "usuario_registro", length = 100)
    private String usuarioRegistro;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "ACTIVO"; // "ACTIVO", "ANULADO"

    @PrePersist
    public void prePersist() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
        if (estado == null) {
            estado = "ACTIVO";
        }
    }
}
