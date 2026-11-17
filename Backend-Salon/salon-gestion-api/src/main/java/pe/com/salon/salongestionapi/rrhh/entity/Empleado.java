package pe.com.salon.salongestionapi.rrhh.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Period;

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

    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    @Column(name = "sueldo_fijo")
    private java.math.BigDecimal sueldoFijo;

    @Column(name = "tipo_remuneracion", length = 30)
    private String tipoRemuneracion = "SOLO_COMISION"; // "SOLO_COMISION", "SOLO_FIJO", "HIBRIDO"

    @Enumerated(EnumType.STRING)
    @Column(name = "disponibilidad", nullable = false)
    private EstadoDisponibilidad disponibilidad = EstadoDisponibilidad.AUSENTE;

    @Column(nullable = false)
    private Boolean estado = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "empleado_especialidad",
        joinColumns = @JoinColumn(name = "empleado_id"),
        inverseJoinColumns = @JoinColumn(name = "especialidad_id")
    )
    private java.util.Set<Especialidad> especialidades = new java.util.HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turno_id")
    private Turno turno;

  

    @Transient
    public String getTipoRemuneracionEfectivo() {
        if (tipoRemuneracion != null && !tipoRemuneracion.isBlank()) {
            return tipoRemuneracion.trim().toUpperCase();
        }
        // Auto-detección para retrocompatibilidad con registros existentes
        if (sueldoFijo != null && sueldoFijo.compareTo(java.math.BigDecimal.ZERO) > 0) {
            return "SOLO_FIJO";
        }
        if (especialidades != null && !especialidades.isEmpty()) {
            boolean tieneFijo = especialidades.stream().anyMatch(e -> Boolean.TRUE.equals(e.getEstado())
                    && ("FIJO".equalsIgnoreCase(e.getTipoPago()) || "Sueldo Fijo Mensual".equalsIgnoreCase(e.getTipoPago())));
            boolean tienePorc = especialidades.stream().anyMatch(e -> Boolean.TRUE.equals(e.getEstado())
                    && ("PORCENTAJE".equalsIgnoreCase(e.getTipoPago()) || "Porcentaje de Comisión".equalsIgnoreCase(e.getTipoPago())));
            if (tieneFijo && tienePorc) {
                return "HIBRIDO";
            }
            if (tieneFijo) {
                return "SOLO_FIJO";
            }
        }
        return "SOLO_COMISION";
    }

    @Transient
    public java.math.BigDecimal getSueldoFijoEfectivo() {
        if (sueldoFijo != null && sueldoFijo.compareTo(java.math.BigDecimal.ZERO) > 0) {
            return sueldoFijo;
        }
        if (especialidades != null) {
            java.math.BigDecimal acum = java.math.BigDecimal.ZERO;
            for (Especialidad e : especialidades) {
                if (Boolean.TRUE.equals(e.getEstado())
                        && ("FIJO".equalsIgnoreCase(e.getTipoPago()) || "Sueldo Fijo Mensual".equalsIgnoreCase(e.getTipoPago()))) {
                    if (e.getMontoFijo() != null) {
                        acum = acum.add(e.getMontoFijo());
                    }
                }
            }
            if (acum.compareTo(java.math.BigDecimal.ZERO) > 0) {
                return acum;
            }
        }
        return java.math.BigDecimal.ZERO;
    }

    // Método utilitario para no guardar la edad en base de datos (ya que cambia cada año)
    @Transient
    public Integer getEdad() {
        if (this.fechaNacimiento == null) {
            return null;
        }
        return Period.between(this.fechaNacimiento, LocalDate.now()).getYears();
    }
}
