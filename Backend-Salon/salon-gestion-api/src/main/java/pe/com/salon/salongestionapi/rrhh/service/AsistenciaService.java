package pe.com.salon.salongestionapi.rrhh.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.salon.salongestionapi.rrhh.dto.AsistenciaRequest;
import pe.com.salon.salongestionapi.rrhh.dto.AsistenciaResponse;
import pe.com.salon.salongestionapi.rrhh.entity.Empleado;
import pe.com.salon.salongestionapi.rrhh.entity.RegistroAsistencia;
import pe.com.salon.salongestionapi.rrhh.entity.TipoAsistencia;
import pe.com.salon.salongestionapi.rrhh.repository.AsistenciaRepository;
import pe.com.salon.salongestionapi.rrhh.repository.EmpleadoRepository;
import pe.com.salon.salongestionapi.shared.exception.ResourceNotFoundException;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AsistenciaService {

    private final AsistenciaRepository asistenciaRepository;
    private final EmpleadoRepository empleadoRepository;

    public AsistenciaResponse registrar(AsistenciaRequest request) {
        Empleado empleado = empleadoRepository.findById(request.getEmpleadoId())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con id: " + request.getEmpleadoId()));

        // Verificar si ya existe registro para ese día
        if (asistenciaRepository.findByEmpleadoIdAndFecha(request.getEmpleadoId(), request.getFecha()).isPresent()) {
            throw new RuntimeException("Ya existe un registro de asistencia para este empleado en la fecha: " + request.getFecha());
        }

        RegistroAsistencia registro = new RegistroAsistencia();
        registro.setEmpleado(empleado);
        registro.setFecha(request.getFecha());
        registro.setHoraEntrada(request.getHoraEntrada());
        registro.setHoraSalida(request.getHoraSalida());
        registro.setTardanzaMinutos(request.getTardanzaMinutos() != null ? request.getTardanzaMinutos() : 0);
        registro.setTipo(request.getTipo());
        registro.setObservaciones(request.getObservaciones());

        // Actualizar disponibilidad del empleado automáticamente
        if (request.getTipo() == TipoAsistencia.ASISTIO || request.getTipo() == TipoAsistencia.TARDANZA) {
            empleado.setDisponibilidad(pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad.DISPONIBLE);
            empleadoRepository.save(empleado);
        }

        RegistroAsistencia guardado = asistenciaRepository.save(registro);
        return mapToResponse(guardado);
    }

    public AsistenciaResponse registrarSalida(Long empleadoId, LocalDate fecha) {
        RegistroAsistencia registro = asistenciaRepository.findByEmpleadoIdAndFecha(empleadoId, fecha)
                .orElseThrow(() -> new ResourceNotFoundException("No existe registro de entrada para este empleado en la fecha indicada"));

        registro.setHoraSalida(java.time.LocalTime.now());

        // Marcar al empleado como Ausente al finalizar su jornada
        Empleado empleado = registro.getEmpleado();
        empleado.setDisponibilidad(pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad.AUSENTE);
        empleadoRepository.save(empleado);

        RegistroAsistencia actualizado = asistenciaRepository.save(registro);
        return mapToResponse(actualizado);
    }

    public List<AsistenciaResponse> listarPorEmpleado(Long empleadoId) {
        return asistenciaRepository.findByEmpleadoIdOrderByFechaDesc(empleadoId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<AsistenciaResponse> listarPorRangoDeFechas(LocalDate inicio, LocalDate fin) {
        return asistenciaRepository.findByFechaBetweenOrderByFechaDesc(inicio, fin)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private AsistenciaResponse mapToResponse(RegistroAsistencia r) {
        AsistenciaResponse res = new AsistenciaResponse();
        res.setId(r.getId());
        res.setFecha(r.getFecha());
        res.setHoraEntrada(r.getHoraEntrada());
        res.setHoraSalida(r.getHoraSalida());
        res.setTardanzaMinutos(r.getTardanzaMinutos());
        res.setTipo(r.getTipo());
        res.setObservaciones(r.getObservaciones());
        res.setEmpleadoId(r.getEmpleado().getId());
        res.setEmpleadoNombreCompleto(r.getEmpleado().getNombres() + " " + r.getEmpleado().getApellidos());
        return res;
    }
}
