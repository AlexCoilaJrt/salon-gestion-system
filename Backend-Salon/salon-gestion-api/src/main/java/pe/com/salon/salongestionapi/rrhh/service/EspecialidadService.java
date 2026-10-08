package pe.com.salon.salongestionapi.rrhh.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.salon.salongestionapi.rrhh.dto.EspecialidadRequest;
import pe.com.salon.salongestionapi.rrhh.dto.EspecialidadResponse;
import pe.com.salon.salongestionapi.rrhh.entity.Especialidad;
import pe.com.salon.salongestionapi.rrhh.repository.EspecialidadRepository;
import pe.com.salon.salongestionapi.shared.exception.ResourceNotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EspecialidadService {

    private final EspecialidadRepository especialidadRepository;

    public List<EspecialidadResponse> listarTodas() {
        return especialidadRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public EspecialidadResponse obtenerPorId(Long id) {
        Especialidad especialidad = especialidadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con id: " + id));
        return mapToResponse(especialidad);
    }

    public EspecialidadResponse crearEspecialidad(EspecialidadRequest request) {
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre(request.getNombre());
        especialidad.setDescripcion(request.getDescripcion());
        especialidad.setTipoPago(request.getTipoPago());
        especialidad.setMontoFijo(request.getMontoFijo());
        especialidad.setPorcentajeComision(request.getPorcentajeComision());
        especialidad.setEstado(request.getEstado() != null ? request.getEstado() : true);

        Especialidad guardada = especialidadRepository.save(especialidad);
        return mapToResponse(guardada);
    }

    public EspecialidadResponse actualizarEspecialidad(Long id, EspecialidadRequest request) {
        Especialidad especialidad = especialidadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con id: " + id));

        especialidad.setNombre(request.getNombre());
        especialidad.setDescripcion(request.getDescripcion());
        especialidad.setTipoPago(request.getTipoPago());
        especialidad.setMontoFijo(request.getMontoFijo());
        especialidad.setPorcentajeComision(request.getPorcentajeComision());
        if (request.getEstado() != null) {
            especialidad.setEstado(request.getEstado());
        }

        Especialidad actualizada = especialidadRepository.save(especialidad);
        return mapToResponse(actualizada);
    }

    public void eliminarEspecialidad(Long id) {
        Especialidad especialidad = especialidadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con id: " + id));
        
        especialidad.setEstado(false);
        especialidadRepository.save(especialidad);
    }

    private EspecialidadResponse mapToResponse(Especialidad especialidad) {
        EspecialidadResponse response = new EspecialidadResponse();
        response.setId(especialidad.getId());
        response.setNombre(especialidad.getNombre());
        response.setDescripcion(especialidad.getDescripcion());
        response.setTipoPago(especialidad.getTipoPago());
        response.setMontoFijo(especialidad.getMontoFijo());
        response.setPorcentajeComision(especialidad.getPorcentajeComision());
        response.setEstado(especialidad.getEstado());
        return response;
    }
}
