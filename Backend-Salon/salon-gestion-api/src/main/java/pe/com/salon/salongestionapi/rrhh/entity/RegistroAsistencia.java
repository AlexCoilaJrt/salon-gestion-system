package pe.com.salon.salongestionapi.rrhh.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "registro_asistencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistroAsistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_entrada")
    private LocalTime horaEntrada;

    @Column(name = "hora_salida")
    private LocalTime horaSalida;

    @Column(name = "hora_inicio_descanso")
    private LocalTime horaInicioDescanso;

    @Column(name = "hora_fin_descanso")
    private LocalTime horaFinDescanso;

    // Minutos de tardanza (0 si llegó a tiempo, null si no aplica)
    @Column(name = "tardanza_minutos")
    private Integer tardanzaMinutos = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoAsistencia tipo = TipoAsistencia.AUSENCIA;

    @Column(length = 255)
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;
}
