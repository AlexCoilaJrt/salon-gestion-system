package pe.com.salon.salongestionapi.rrhh.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.salon.salongestionapi.rrhh.dto.EmpleadoRequest;
import pe.com.salon.salongestionapi.rrhh.dto.EmpleadoResponse;
import pe.com.salon.salongestionapi.rrhh.entity.Empleado;
import pe.com.salon.salongestionapi.rrhh.entity.Especialidad;
import pe.com.salon.salongestionapi.rrhh.repository.EmpleadoRepository;
import pe.com.salon.salongestionapi.rrhh.repository.EspecialidadRepository;
import pe.com.salon.salongestionapi.shared.exception.ResourceNotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final EspecialidadRepository especialidadRepository;

    public List<EmpleadoResponse> listarTodos() {
        return empleadoRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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
        empleado.setEstado(true);

        if (request.getEspecialidadId() != null) {
            Especialidad especialidad = especialidadRepository.findById(request.getEspecialidadId())
                    .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con id: " + request.getEspecialidadId()));
            empleado.setEspecialidad(especialidad);
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

        if (request.getEspecialidadId() != null) {
            Especialidad especialidad = especialidadRepository.findById(request.getEspecialidadId())
                    .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con id: " + request.getEspecialidadId()));
            empleado.setEspecialidad(especialidad);
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
        if (empleado.getEspecialidad() != null) {
            response.setEspecialidadNombre(empleado.getEspecialidad().getNombre());
        }
        return response;
    }
}
