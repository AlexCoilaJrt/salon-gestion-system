package pe.com.salon.salongestionapi.rrhh.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Period;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "empleado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nombres;

    @Column(nullable = false, length = 60)
    private String apellidos;

    @Column(nullable = false, unique = true, length = 15)
    private String dni;

    @Column(length = 100, unique = true)
    private String email;

    @Column(length = 15)
    private String telefono;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "disponibilidad", nullable = false)
    private EstadoDisponibilidad disponibilidad = EstadoDisponibilidad.AUSENTE;

    @Column(nullable = false)
    private Boolean estado = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especialidad_id")
    private Especialidad especialidad;

    // Método utilitario para no guardar la edad en base de datos (ya que cambia cada año)
    @Transient
    public Integer getEdad() {
        if (this.fechaNacimiento == null) {
            return null;
        }
        return Period.between(this.fechaNacimiento, LocalDate.now()).getYears();
    }
}
