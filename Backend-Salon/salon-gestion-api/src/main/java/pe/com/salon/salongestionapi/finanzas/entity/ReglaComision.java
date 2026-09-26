package pe.com.salon.salongestionapi.finanzas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.salon.salongestionapi.rrhh.entity.Especialidad;

import java.math.BigDecimal;

@Entity
@Table(name = "regla_comision")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReglaComision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre; // Ej: "Comisión Estándar Estilista"

    // Guardaremos el porcentaje como un número entero (Ej: 50 para 50%)
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especialidad_id")
    private Especialidad especialidad; // Si es null, aplica a todos

    @Column(nullable = false)
    private Boolean estado = true;
}
