package pe.com.salon.salongestionapi.rrhh.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "especialidad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Especialidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(length = 200)
    private String descripcion;

    @Column(length = 20)
    private String tipoPago; // "FIJO" o "PORCENTAJE"

    @Column(name = "monto_fijo", precision = 10, scale = 2)
    private java.math.BigDecimal montoFijo;

    @Column(name = "porcentaje_comision", precision = 5, scale = 2)
    private java.math.BigDecimal porcentajeComision;

    @Column(nullable = false)
    private Boolean estado = true;
}
