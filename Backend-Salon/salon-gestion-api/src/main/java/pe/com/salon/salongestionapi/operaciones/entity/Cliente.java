package pe.com.salon.salongestionapi.operaciones.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nombres;

    @Column(nullable = false, length = 60)
    private String apellidos;

    @Column(length = 20)
    private String telefono;

    @Column(length = 100)
    private String email;

    // GAP 7: Cumpleaños del cliente
    @Column(name = "fecha_nacimiento")
    private java.time.LocalDate fechaNacimiento;

    // GAP 6: Fidelización
    @Column(name = "puntos_fidelizacion", nullable = false)
    private Integer puntosFidelizacion = 0;

    @Column(name = "fecha_ultima_visita")
    private java.time.LocalDate fechaUltimaVisita;

    @Column(nullable = false)
    private Boolean estado = true;
}
