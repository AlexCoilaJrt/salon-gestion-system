package pe.com.salon.salongestionapi.finanzas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "dia_especial")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DiaEspecial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String descripcion; // Ej: "Día de la Madre", "Navidad"

    @Column(nullable = false)
    private LocalDate fecha;

    // Porcentaje adicional de bono que se suma a la comisión ese día
    // Ej: 2.00 = 2% extra
    @Column(name = "bono_porcentaje", nullable = false, precision = 5, scale = 2)
    private BigDecimal bonoPorcentaje = BigDecimal.ZERO;
}
