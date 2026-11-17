package pe.com.salon.salongestionapi.rrhh.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import pe.com.salon.salongestionapi.rrhh.dto.EmpleadoRequest;
import pe.com.salon.salongestionapi.rrhh.dto.EmpleadoResponse;
import pe.com.salon.salongestionapi.rrhh.entity.Empleado;
import pe.com.salon.salongestionapi.rrhh.entity.Especialidad;
import pe.com.salon.salongestionapi.rrhh.repository.EmpleadoRepository;
import pe.com.salon.salongestionapi.rrhh.repository.EspecialidadRepository;
import pe.com.salon.salongestionapi.shared.PageResponse;
import pe.com.salon.salongestionapi.shared.exception.ResourceNotFoundException;
import pe.com.salon.salongestionapi.exception.BadRequestException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final EspecialidadRepository especialidadRepository;
    private final pe.com.salon.salongestionapi.rrhh.repository.TurnoRepository turnoRepository;
    private final pe.com.salon.salongestionapi.rrhh.repository.AsistenciaRepository asistenciaRepository;

    private static final java.time.ZoneId ZONE_PERU = java.time.ZoneId.of("America/Lima");

    public List<EmpleadoResponse> listarTodos() {
        return empleadoRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public PageResponse<EmpleadoResponse> listarPaginado(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        // IMPORTANTE: pasar "" en lugar de null para evitar error lower(bytea) en PostgreSQL
        String searchTerm = (search != null && !search.isBlank()) ? search.trim() : "";
        Page<Empleado> resultado = empleadoRepository.buscarPaginado(searchTerm, pageable);
        List<EmpleadoResponse> content = resultado.getContent().stream()
                .map(this::mapToResponse)
                .sorted((a, b) -> {
                    if (a.getEstado() == null || b.getEstado() == null) return 0;
                    if (a.getEstado().equals(b.getEstado())) return 0;
                    return a.getEstado() ? -1 : 1;
                })
                .collect(Collectors.toList());
        return PageResponse.<EmpleadoResponse>builder()
                .content(content)
                .pageNumber(resultado.getNumber())
                .pageSize(resultado.getSize())
                .totalElements(resultado.getTotalElements())
                .totalPages(resultado.getTotalPages())
                .isLast(resultado.isLast())
                .build();
    }

    public EmpleadoResponse crearEmpleado(EmpleadoRequest request) {
        String emailVal = (request.getEmail() != null && !request.getEmail().trim().isEmpty()) ? request.getEmail().trim() : null;
        if (emailVal != null && empleadoRepository.existsByEmail(emailVal)) {
            throw new BadRequestException("El correo electrónico ya está registrado: " + emailVal);
        }
        if (request.getDni() != null && empleadoRepository.existsByDni(request.getDni())) {
            throw new BadRequestException("El DNI ya está registrado: " + request.getDni());
        }

        Empleado empleado = new Empleado();
        empleado.setNombres(request.getNombres());
        empleado.setApellidos(request.getApellidos());
        empleado.setDni(request.getDni());
        empleado.setEmail(emailVal);
        empleado.setTelefono(request.getTelefono());
        empleado.setFechaNacimiento(request.getFechaNacimiento());
        empleado.setFechaIngreso(request.getFechaIngreso());
        String tipoRem = (request.getTipoRemuneracion() != null && !request.getTipoRemuneracion().isBlank())
                ? request.getTipoRemuneracion().trim().toUpperCase() : "SOLO_COMISION";
        empleado.setTipoRemuneracion(tipoRem);
        empleado.setSueldoFijo("SOLO_COMISION".equalsIgnoreCase(tipoRem) ? java.math.BigDecimal.ZERO : request.getSueldoFijo());
        empleado.setDisponibilidad(pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad.AUSENTE);
        empleado.setEstado(request.getEstado() != null ? request.getEstado() : true);

        if (request.getEspecialidadIds() != null && !request.getEspecialidadIds().isEmpty()) {
            java.util.List<Especialidad> especialidades = especialidadRepository.findAllById(request.getEspecialidadIds());
            if (especialidades.size() != request.getEspecialidadIds().size()) {
                throw new ResourceNotFoundException("Una o más especialidades no fueron encontradas");
            }
            empleado.setEspecialidades(new java.util.HashSet<>(especialidades));
        }
        
        if (request.getTurnoId() != null) {
            pe.com.salon.salongestionapi.rrhh.entity.Turno turno = turnoRepository.findById(request.getTurnoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado"));
            empleado.setTurno(turno);
        }

        Empleado guardado = empleadoRepository.save(empleado);
        return mapToResponse(guardado);
    }

    public EmpleadoResponse obtenerPorId(Long id) {
        Empleado empleado = empleadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con id: " + id));
        return mapToResponse(empleado);
    }

    public EmpleadoResponse actualizarEmpleado(Long id, EmpleadoRequest request) {
        Empleado empleado = empleadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con id: " + id));

        String emailVal = (request.getEmail() != null && !request.getEmail().trim().isEmpty()) ? request.getEmail().trim() : null;
        if (emailVal != null && !emailVal.equalsIgnoreCase(empleado.getEmail()) && empleadoRepository.existsByEmail(emailVal)) {
            throw new BadRequestException("El correo electrónico ya está registrado por otro empleado: " + emailVal);
        }
        
        if (request.getDni() != null && !request.getDni().equals(empleado.getDni()) 
            && empleadoRepository.existsByDni(request.getDni())) {
            throw new BadRequestException("El DNI ya está registrado por otro empleado: " + request.getDni());
        }

        empleado.setNombres(request.getNombres());
        empleado.setApellidos(request.getApellidos());
        empleado.setDni(request.getDni());
        empleado.setEmail(emailVal);
        empleado.setTelefono(request.getTelefono());
        empleado.setFechaNacimiento(request.getFechaNacimiento());
        empleado.setFechaIngreso(request.getFechaIngreso());
        if (request.getTipoRemuneracion() != null && !request.getTipoRemuneracion().isBlank()) {
            String tipoRem = request.getTipoRemuneracion().trim().toUpperCase();
            empleado.setTipoRemuneracion(tipoRem);
            empleado.setSueldoFijo("SOLO_COMISION".equalsIgnoreCase(tipoRem) ? java.math.BigDecimal.ZERO : request.getSueldoFijo());
        } else if (request.getSueldoFijo() != null) {
            empleado.setSueldoFijo(request.getSueldoFijo());
        }
        if (request.getEstado() != null) {
            empleado.setEstado(request.getEstado());
        }

        if (request.getEspecialidadIds() != null) {
            java.util.List<Especialidad> especialidades = especialidadRepository.findAllById(request.getEspecialidadIds());
            if (especialidades.size() != request.getEspecialidadIds().size()) {
                throw new ResourceNotFoundException("Una o más especialidades no fueron encontradas");
            }
            empleado.setEspecialidades(new java.util.HashSet<>(especialidades));
        } else {
            empleado.getEspecialidades().clear();
        }

        if (request.getTurnoId() != null) {
            pe.com.salon.salongestionapi.rrhh.entity.Turno turno = turnoRepository.findById(request.getTurnoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado"));
            empleado.setTurno(turno);
        } else {
            empleado.setTurno(null);
        }

        Empleado actualizado = empleadoRepository.save(empleado);
        return mapToResponse(actualizado);
    }

    public void eliminarEmpleado(Long id) {
        Empleado empleado = empleadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con id: " + id));
        
        // Eliminación lógica (soft delete) cambiando el estado a false
        empleado.setEstado(false);
        empleadoRepository.save(empleado);
    }

    public pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad evaluarDisponibilidadEfectiva(Empleado empleado) {
        if (!Boolean.TRUE.equals(empleado.getEstado())) {
            return actualizarSiCambio(empleado, pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad.AUSENTE);
        }
        pe.com.salon.salongestionapi.rrhh.entity.Turno turno = empleado.getTurno();
        if (turno == null || !Boolean.TRUE.equals(turno.getEstado())) {
            return actualizarSiCambio(empleado, pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad.AUSENTE);
        }

        java.time.LocalDate hoy = java.time.LocalDate.now(ZONE_PERU);
        java.time.LocalTime ahora = java.time.LocalTime.now(ZONE_PERU);

        java.util.Optional<pe.com.salon.salongestionapi.rrhh.entity.RegistroAsistencia> asisOpt =
                empleado.getId() != null ? asistenciaRepository.findByEmpleadoIdAndFecha(empleado.getId(), hoy) : java.util.Optional.empty();

        if (asisOpt.isPresent()) {
            pe.com.salon.salongestionapi.rrhh.entity.RegistroAsistencia asis = asisOpt.get();
            if (asis.getHoraSalida() != null) {
                return actualizarSiCambio(empleado, pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad.AUSENTE);
            }
            if (asis.getHoraInicioDescanso() != null && asis.getHoraFinDescanso() == null) {
                return actualizarSiCambio(empleado, pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad.EN_DESCANSO);
            }
        }

        // Evaluar si está en horario de refrigerio del turno
        if (turno.getHoraInicioRefrigerio() != null && turno.getHoraFinRefrigerio() != null) {
            if (estaEnRangoHorario(ahora, turno.getHoraInicioRefrigerio(), turno.getHoraFinRefrigerio())) {
                return actualizarSiCambio(empleado, pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad.EN_DESCANSO);
            }
        }

        // Evaluar si está dentro del horario del turno (o con asistencia abierta)
        boolean enHorarioTurno = turno.getHoraEntrada() != null && turno.getHoraSalida() != null
                && estaEnRangoHorario(ahora, turno.getHoraEntrada(), turno.getHoraSalida());
        boolean asistenciaAbierta = asisOpt.isPresent()
                && asisOpt.get().getHoraEntrada() != null
                && asisOpt.get().getHoraSalida() == null;

        if (enHorarioTurno || asistenciaAbierta) {
            pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad target =
                    empleado.getDisponibilidad() == pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad.EN_ATENCION
                            ? pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad.EN_ATENCION
                            : pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad.DISPONIBLE;
            return actualizarSiCambio(empleado, target);
        }

        return actualizarSiCambio(empleado, pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad.AUSENTE);
    }

    private boolean estaEnRangoHorario(java.time.LocalTime actual, java.time.LocalTime inicio, java.time.LocalTime fin) {
        if (inicio == null || fin == null) return false;
        if (!inicio.isAfter(fin)) {
            return !actual.isBefore(inicio) && !actual.isAfter(fin);
        } else {
            // Turno que cruza medianoche
            return !actual.isBefore(inicio) || !actual.isAfter(fin);
        }
    }

    private pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad actualizarSiCambio(
            Empleado empleado, pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad nueva) {
        if (empleado.getDisponibilidad() != nueva) {
            empleado.setDisponibilidad(nueva);
            if (empleado.getId() != null) {
                try {
                    empleadoRepository.save(empleado);
                } catch (Exception ignored) {
                }
            }
        }
        return nueva;
    }

    public void validarEmpleadoHabilitadoParaTicket(Empleado empleado) {
        if (empleado == null) return;
        String nombre = (empleado.getNombres() + " " + (empleado.getApellidos() != null ? empleado.getApellidos() : "")).trim();
        if (!Boolean.TRUE.equals(empleado.getEstado())) {
            throw new BadRequestException("No se puede asignar el ticket: El especialista " + nombre + " se encuentra en estado INACTIVO.");
        }
        pe.com.salon.salongestionapi.rrhh.entity.Turno turno = empleado.getTurno();
        if (turno == null || !Boolean.TRUE.equals(turno.getEstado())) {
            throw new BadRequestException("No se puede asignar el ticket: El especialista " + nombre + " no tiene un turno de trabajo activo asignado.");
        }
        pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad disp = evaluarDisponibilidadEfectiva(empleado);
        if (disp == pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad.EN_DESCANSO) {
            String rangoRef = (turno.getHoraInicioRefrigerio() != null && turno.getHoraFinRefrigerio() != null)
                    ? " (" + turno.getHoraInicioRefrigerio() + " - " + turno.getHoraFinRefrigerio() + ")" : "";
            throw new BadRequestException("No se puede asignar el ticket: El especialista " + nombre + " está en su hora de REFRIGERIO / DESCANSO" + rangoRef + ".");
        }
        if (disp == pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad.AUSENTE) {
            throw new BadRequestException("No se puede asignar el ticket: El especialista " + nombre + " se encuentra FUERA DE TURNO / AUSENTE (Turno " + turno.getNombre() + ": " + turno.getHoraEntrada() + " a " + turno.getHoraSalida() + ").");
        }
    }

    private EmpleadoResponse mapToResponse(Empleado empleado) {
        pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad dispEfectiva = evaluarDisponibilidadEfectiva(empleado);
        EmpleadoResponse response = new EmpleadoResponse();
        response.setId(empleado.getId());
        response.setNombres(empleado.getNombres());
        response.setApellidos(empleado.getApellidos());
        response.setDni(empleado.getDni());
        response.setEmail(empleado.getEmail());
        response.setTelefono(empleado.getTelefono());
        response.setFechaNacimiento(empleado.getFechaNacimiento());
        response.setFechaIngreso(empleado.getFechaIngreso());
        response.setSueldoFijo(empleado.getSueldoFijoEfectivo());
        response.setTipoRemuneracion(empleado.getTipoRemuneracionEfectivo());
        response.setEdad(empleado.getEdad());
        response.setDisponibilidad(dispEfectiva.name());
        response.setEstado(empleado.getEstado());

        if (!Boolean.TRUE.equals(empleado.getEstado())) {
            response.setEstadoHorario("INACTIVO");
            response.setHabilitadoParaAtencion(false);
            response.setMotivoNoDisponible("Empleado Inactivo");
        } else if (empleado.getTurno() == null || !Boolean.TRUE.equals(empleado.getTurno().getEstado())) {
            response.setEstadoHorario("SIN_TURNO");
            response.setHabilitadoParaAtencion(false);
            response.setMotivoNoDisponible("Sin turno asignado");
        } else if (dispEfectiva == pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad.EN_DESCANSO) {
            response.setEstadoHorario("EN_REFRIGERIO");
            response.setHabilitadoParaAtencion(false);
            response.setMotivoNoDisponible("En hora de refrigerio / descanso");
        } else if (dispEfectiva == pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad.AUSENTE) {
            response.setEstadoHorario("FUERA_DE_TURNO");
            response.setHabilitadoParaAtencion(false);
            response.setMotivoNoDisponible("Fuera de su horario de turno (" + empleado.getTurno().getHoraEntrada() + " - " + empleado.getTurno().getHoraSalida() + ")");
        } else {
            response.setEstadoHorario("EN_TURNO");
            response.setHabilitadoParaAtencion(true);
            response.setMotivoNoDisponible(null);
        }
        
        if (empleado.getTurno() != null) {
            response.setTurnoId(empleado.getTurno().getId());
            response.setTurnoNombre(empleado.getTurno().getNombre());
        }
        
        if (empleado.getEspecialidades() != null && !empleado.getEspecialidades().isEmpty()) {
            response.setEspecialidadesNombres(empleado.getEspecialidades().stream().map(Especialidad::getNombre).collect(Collectors.toList()));
            response.setEspecialidadIds(empleado.getEspecialidades().stream().map(Especialidad::getId).collect(Collectors.toList()));
        } else {
            response.setEspecialidadesNombres(new java.util.ArrayList<>());
            response.setEspecialidadIds(new java.util.ArrayList<>());
        }
        return response;
    }
}
