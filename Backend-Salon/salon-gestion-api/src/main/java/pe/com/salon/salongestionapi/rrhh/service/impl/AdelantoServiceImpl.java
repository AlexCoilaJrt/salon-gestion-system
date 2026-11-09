package pe.com.salon.salongestionapi.rrhh.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.salon.salongestionapi.operaciones.entity.Egreso;
import pe.com.salon.salongestionapi.operaciones.entity.SesionCaja;
import pe.com.salon.salongestionapi.operaciones.repository.EgresoRepository;
import pe.com.salon.salongestionapi.operaciones.repository.SesionCajaRepository;
import pe.com.salon.salongestionapi.rrhh.dto.AdelantoRequest;
import pe.com.salon.salongestionapi.rrhh.dto.AdelantoResponse;
import pe.com.salon.salongestionapi.rrhh.entity.Adelanto;
import pe.com.salon.salongestionapi.rrhh.entity.Empleado;
import pe.com.salon.salongestionapi.rrhh.repository.AdelantoRepository;
import pe.com.salon.salongestionapi.rrhh.repository.EmpleadoRepository;
import pe.com.salon.salongestionapi.rrhh.service.AdelantoService;
import pe.com.salon.salongestionapi.security.entity.Usuario;
import pe.com.salon.salongestionapi.security.repository.UsuarioRepository;
import pe.com.salon.salongestionapi.shared.exception.ResourceNotFoundException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdelantoServiceImpl implements AdelantoService {

    private final AdelantoRepository adelantoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final SesionCajaRepository sesionCajaRepository;
    private final EgresoRepository egresoRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public AdelantoResponse registrarAdelanto(AdelantoRequest request) {
        Empleado empleado = empleadoRepository.findById(request.getEmpleadoId())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con ID: " + request.getEmpleadoId()));

        String username = "SISTEMA";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            username = auth.getName();
        }

        Long egresoId = null;
        if (Boolean.TRUE.equals(request.getDescontarDeCaja())) {
            SesionCaja sesionAbierta = sesionCajaRepository.findByEstadoTrue()
                    .orElseThrow(() -> new ResourceNotFoundException("No hay una caja abierta para registrar la salida de efectivo del adelanto."));

            Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);

            Egreso egreso = new Egreso();
            egreso.setMonto(request.getMonto());
            egreso.setCategoria("Adelantos a Personal");
            egreso.setMotivo("Adelanto a " + empleado.getNombres() + " " + empleado.getApellidos() + ": " + request.getMotivo());
            egreso.setFechaHora(LocalDateTime.now());
            egreso.setSesionCaja(sesionAbierta);
            egreso.setUsuario(usuario);

            Egreso egresoGuardado = egresoRepository.save(egreso);
            egresoId = egresoGuardado.getId();
        }

        Adelanto adelanto = Adelanto.builder()
                .empleado(empleado)
                .monto(request.getMonto())
                .fecha(request.getFecha() != null ? request.getFecha() : LocalDate.now())
                .metodoPago(request.getMetodoPago() != null ? request.getMetodoPago().toUpperCase() : "EFECTIVO")
                .motivo(request.getMotivo())
                .egresoId(egresoId)
                .usuarioRegistro(username)
                .fechaRegistro(LocalDateTime.now())
                .estado("ACTIVO")
                .build();

        Adelanto guardado = adelantoRepository.save(adelanto);
        return mapToResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdelantoResponse> obtenerAdelantosPorEmpleadoYPeriodo(Long empleadoId, LocalDate inicio, LocalDate fin) {
        if (inicio != null && fin != null) {
            return adelantoRepository.findByEmpleadoIdAndFechaBetweenAndEstadoOrderByFechaDesc(empleadoId, inicio, fin, "ACTIVO")
                    .stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } else {
            return adelantoRepository.findByEmpleadoIdAndEstadoOrderByFechaDesc(empleadoId, "ACTIVO")
                    .stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }
    }

    @Override
    @Transactional
    public AdelantoResponse anularAdelanto(Long id) {
        Adelanto adelanto = adelantoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Adelanto no encontrado con ID: " + id));

        adelanto.setEstado("ANULADO");
        Adelanto guardado = adelantoRepository.save(adelanto);
        return mapToResponse(guardado);
    }

    @Override
    @Transactional
    public AdelantoResponse actualizarAdelanto(Long id, AdelantoRequest request) {
        Adelanto adelanto = adelantoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Adelanto no encontrado con ID: " + id));

        if (request.getMonto() != null) {
            adelanto.setMonto(request.getMonto());
        }
        if (request.getFecha() != null) {
            adelanto.setFecha(request.getFecha());
        }
        if (request.getMetodoPago() != null && !request.getMetodoPago().isBlank()) {
            adelanto.setMetodoPago(request.getMetodoPago().toUpperCase());
        }
        adelanto.setMotivo(request.getMotivo() != null ? request.getMotivo().trim() : null);

        // Si estaba asociado a una salida de caja, actualizar el monto y motivo del egreso
        if (adelanto.getEgresoId() != null) {
            egresoRepository.findById(adelanto.getEgresoId()).ifPresent(eg -> {
                if (request.getMonto() != null) {
                    eg.setMonto(request.getMonto());
                }
                eg.setMotivo("Adelanto a " + (adelanto.getEmpleado() != null ? adelanto.getEmpleado().getNombres() : "") + ": " + (request.getMotivo() != null ? request.getMotivo().trim() : ""));
                egresoRepository.save(eg);
            });
        }

        Adelanto guardado = adelantoRepository.save(adelanto);
        return mapToResponse(guardado);
    }

    @Override
    @Transactional
    public void eliminarAdelanto(Long id) {
        Adelanto adelanto = adelantoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Adelanto no encontrado con ID: " + id));

        if (adelanto.getEgresoId() != null) {
            try {
                egresoRepository.deleteById(adelanto.getEgresoId());
            } catch (Exception ignored) {
            }
        }

        adelantoRepository.delete(adelanto);
    }

    private AdelantoResponse mapToResponse(Adelanto a) {
        String nombreCompleto = a.getEmpleado() != null 
                ? (a.getEmpleado().getNombres() + " " + a.getEmpleado().getApellidos()) 
                : "";

        return AdelantoResponse.builder()
                .id(a.getId())
                .empleadoId(a.getEmpleado() != null ? a.getEmpleado().getId() : null)
                .empleadoNombreCompleto(nombreCompleto)
                .monto(a.getMonto())
                .fecha(a.getFecha())
                .metodoPago(a.getMetodoPago())
                .motivo(a.getMotivo())
                .egresoId(a.getEgresoId())
                .usuarioRegistro(a.getUsuarioRegistro())
                .fechaRegistro(a.getFechaRegistro())
                .estado(a.getEstado())
                .build();
    }
}

