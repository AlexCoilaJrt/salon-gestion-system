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

    public pe.com.salon.salongestionapi.rrhh.dto.KioskoResponse marcarKiosko(pe.com.salon.salongestionapi.rrhh.dto.KioskoRequest request) {
        Empleado empleado = empleadoRepository.findByDni(request.getDni())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con DNI: " + request.getDni()));

        if (!empleado.getEstado()) {
            throw new RuntimeException("El empleado se encuentra Inactivo en el sistema.");
        }

        LocalDate hoy = LocalDate.now();
        java.util.Optional<RegistroAsistencia> registroOpt = asistenciaRepository.findByEmpleadoIdAndFecha(empleado.getId(), hoy);

        String accion = request.getAccion() != null ? request.getAccion().toUpperCase() : "ASISTENCIA";

        if (registroOpt.isPresent()) {
            RegistroAsistencia registro = registroOpt.get();
            
            if ("DESCANSO".equals(accion)) {
                if (registro.getHoraSalida() != null) {
                    throw new RuntimeException("Ya has marcado tu salida, no puedes tomar descanso.");
                }
                if (registro.getHoraInicioDescanso() == null) {
                    registro.setHoraInicioDescanso(java.time.LocalTime.now());
                    empleado.setDisponibilidad(pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad.EN_DESCANSO);
                    empleadoRepository.save(empleado);
                    asistenciaRepository.save(registro);
                    return pe.com.salon.salongestionapi.rrhh.dto.KioskoResponse.builder()
                            .mensaje("¡Descanso Iniciado! Buen provecho.")
                            .empleadoNombre(empleado.getNombres())
                            .tipoRegistro("INICIO_DESCANSO")
                            .horaRegistro(registro.getHoraInicioDescanso())
                            .build();
                } else if (registro.getHoraFinDescanso() == null) {
                    registro.setHoraFinDescanso(java.time.LocalTime.now());
                    empleado.setDisponibilidad(pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad.DISPONIBLE);
                    empleadoRepository.save(empleado);
                    asistenciaRepository.save(registro);
                    return pe.com.salon.salongestionapi.rrhh.dto.KioskoResponse.builder()
                            .mensaje("¡Fin de Descanso! Bienvenido de vuelta.")
                            .empleadoNombre(empleado.getNombres())
                            .tipoRegistro("FIN_DESCANSO")
                            .horaRegistro(registro.getHoraFinDescanso())
                            .build();
                } else {
                    throw new RuntimeException("Ya tomaste tu descanso por hoy.");
                }
            } else {
                if (registro.getHoraSalida() != null) {
                    throw new RuntimeException("Ya has registrado tu entrada y salida por hoy.");
                }
                if (registro.getHoraInicioDescanso() != null && registro.getHoraFinDescanso() == null) {
                    registro.setHoraFinDescanso(java.time.LocalTime.now());
                }
                
                registro.setHoraSalida(java.time.LocalTime.now());
                empleado.setDisponibilidad(pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad.AUSENTE);
                empleadoRepository.save(empleado);
                asistenciaRepository.save(registro);

                return pe.com.salon.salongestionapi.rrhh.dto.KioskoResponse.builder()
                        .mensaje("¡Hasta luego! Salida registrada exitosamente.")
                        .empleadoNombre(empleado.getNombres())
                        .tipoRegistro("SALIDA")
                        .horaRegistro(registro.getHoraSalida())
                        .build();
            }
        } else {
            if ("DESCANSO".equals(accion)) {
                throw new RuntimeException("Debes marcar tu entrada antes de tomar un descanso.");
            }
            // Registrar Entrada
            java.time.LocalTime horaActual = java.time.LocalTime.now();
            int tardanzaMinutos = 0;
            TipoAsistencia tipo = TipoAsistencia.ASISTIO;
            
            if (empleado.getTurno() != null) {
                java.time.LocalTime horaEsperada = empleado.getTurno().getHoraEntrada();
                int tolerancia = empleado.getTurno().getToleranciaMinutos();
                
                long minutosRetraso = java.time.temporal.ChronoUnit.MINUTES.between(horaEsperada, horaActual);
                if (minutosRetraso > tolerancia) {
                    tardanzaMinutos = (int) minutosRetraso;
                    tipo = TipoAsistencia.TARDANZA;
                }
            }

            RegistroAsistencia nuevoRegistro = new RegistroAsistencia();
            nuevoRegistro.setEmpleado(empleado);
            nuevoRegistro.setFecha(hoy);
            nuevoRegistro.setHoraEntrada(horaActual);
            nuevoRegistro.setTipo(tipo);
            nuevoRegistro.setTardanzaMinutos(tardanzaMinutos);
            
            empleado.setDisponibilidad(pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad.DISPONIBLE);
            empleadoRepository.save(empleado);
            asistenciaRepository.save(nuevoRegistro);

            return pe.com.salon.salongestionapi.rrhh.dto.KioskoResponse.builder()
                    .mensaje("¡Bienvenido! Entrada registrada exitosamente.")
                    .empleadoNombre(empleado.getNombres())
                    .tipoRegistro("ENTRADA")
                    .horaRegistro(nuevoRegistro.getHoraEntrada())
                    .build();
        }
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
        
        if (r.getHoraEntrada() != null && r.getHoraSalida() != null) {
            long totalMinutes = java.time.temporal.ChronoUnit.MINUTES.between(r.getHoraEntrada(), r.getHoraSalida());
            long hours = totalMinutes / 60;
            long minutes = totalMinutes % 60;
            res.setHorasTrabajadas(String.format("%dh %02dm", hours, minutes));
        } else {
            res.setHorasTrabajadas("-");
        }
        
        return res;
    }
}
