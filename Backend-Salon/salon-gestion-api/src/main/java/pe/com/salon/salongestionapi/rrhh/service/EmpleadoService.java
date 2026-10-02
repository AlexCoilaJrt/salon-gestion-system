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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final EspecialidadRepository especialidadRepository;
    private final pe.com.salon.salongestionapi.rrhh.repository.TurnoRepository turnoRepository;

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
        Empleado empleado = new Empleado();
        empleado.setNombres(request.getNombres());
        empleado.setApellidos(request.getApellidos());
        empleado.setDni(request.getDni());
        empleado.setEmail(request.getEmail());
        empleado.setTelefono(request.getTelefono());
        empleado.setFechaNacimiento(request.getFechaNacimiento());
        empleado.setDisponibilidad(pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad.AUSENTE);
        empleado.setEstado(request.getEstado() != null ? request.getEstado() : true);
        empleado.setSueldoFijo(request.getSueldoFijo() != null ? request.getSueldoFijo() : java.math.BigDecimal.ZERO);

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

        empleado.setNombres(request.getNombres());
        empleado.setApellidos(request.getApellidos());
        empleado.setDni(request.getDni());
        empleado.setEmail(request.getEmail());
        empleado.setTelefono(request.getTelefono());
        empleado.setFechaNacimiento(request.getFechaNacimiento());
        if (request.getEstado() != null) {
            empleado.setEstado(request.getEstado());
        }
        if (request.getSueldoFijo() != null) {
            empleado.setSueldoFijo(request.getSueldoFijo());
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

    private EmpleadoResponse mapToResponse(Empleado empleado) {
        EmpleadoResponse response = new EmpleadoResponse();
        response.setId(empleado.getId());
        response.setNombres(empleado.getNombres());
        response.setApellidos(empleado.getApellidos());
        response.setDni(empleado.getDni());
        response.setEmail(empleado.getEmail());
        response.setTelefono(empleado.getTelefono());
        response.setFechaNacimiento(empleado.getFechaNacimiento());
        response.setEdad(empleado.getEdad());
        response.setDisponibilidad(empleado.getDisponibilidad().name());
        response.setEstado(empleado.getEstado());
        response.setSueldoFijo(empleado.getSueldoFijo());
        
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
