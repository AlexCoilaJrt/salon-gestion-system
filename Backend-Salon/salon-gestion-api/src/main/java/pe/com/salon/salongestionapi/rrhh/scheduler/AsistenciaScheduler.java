package pe.com.salon.salongestionapi.rrhh.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pe.com.salon.salongestionapi.rrhh.entity.Empleado;
import pe.com.salon.salongestionapi.rrhh.entity.RegistroAsistencia;
import pe.com.salon.salongestionapi.rrhh.entity.TipoAsistencia;
import pe.com.salon.salongestionapi.rrhh.repository.AsistenciaRepository;
import pe.com.salon.salongestionapi.rrhh.repository.EmpleadoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsistenciaScheduler {

    private final EmpleadoRepository empleadoRepository;
    private final AsistenciaRepository asistenciaRepository;

    /**
     * Se ejecuta todos los días a las 23:59:00
     * Revisa qué empleados con estado ACTIVO y que tengan un Turno asignado
     * no han marcado asistencia el día de hoy, y les genera un registro de AUSENCIA.
     */
    @Scheduled(cron = "0 59 23 * * ?")
    public void procesarAusenciasDiarias() {
        log.info("Iniciando procesamiento automático de ausencias...");
        LocalDate hoy = LocalDate.now();

        // Obtener todos los empleados activos
        List<Empleado> empleadosActivos = empleadoRepository.findByEstadoTrue();

        int ausenciasGeneradas = 0;

        for (Empleado empleado : empleadosActivos) {
            // Solo procesamos empleados que tengan un turno asignado (es decir, deberían trabajar)
            if (empleado.getTurno() != null) {
                // Verificamos si tiene un registro de asistencia para hoy
                Optional<RegistroAsistencia> asistenciaOpt = asistenciaRepository.findByEmpleadoIdAndFecha(empleado.getId(), hoy);
                
                if (asistenciaOpt.isEmpty()) {
                    // El empleado no marcó asistencia hoy -> Registrar Ausencia
                    RegistroAsistencia ausencia = new RegistroAsistencia();
                    ausencia.setEmpleado(empleado);
                    ausencia.setFecha(hoy);
                    ausencia.setTipo(TipoAsistencia.AUSENCIA);
                    ausencia.setTardanzaMinutos(0);
                    ausencia.setObservaciones("Ausencia generada automáticamente por el sistema.");

                    asistenciaRepository.save(ausencia);
                    ausenciasGeneradas++;
                    log.info("Ausencia generada para el empleado: {} {}", empleado.getNombres(), empleado.getApellidos());
                }
            }
        }

        log.info("Procesamiento de ausencias finalizado. Se generaron {} ausencias para el día {}.", ausenciasGeneradas, hoy);
    }
}
