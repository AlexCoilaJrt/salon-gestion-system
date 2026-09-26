package pe.com.salon.salongestionapi.finanzas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.salon.salongestionapi.finanzas.dto.TramoComisionRequest;
import pe.com.salon.salongestionapi.finanzas.dto.TramoComisionResponse;
import pe.com.salon.salongestionapi.finanzas.entity.TramoComision;
import pe.com.salon.salongestionapi.finanzas.repository.TramoComisionRepository;
import pe.com.salon.salongestionapi.rrhh.entity.Especialidad;
import pe.com.salon.salongestionapi.rrhh.repository.EspecialidadRepository;
import pe.com.salon.salongestionapi.shared.exception.ResourceNotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TramoComisionService {

    private final TramoComisionRepository tramoComisionRepository;
    private final EspecialidadRepository especialidadRepository;

    public List<TramoComisionResponse> listarTodos() {
        return tramoComisionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public TramoComisionResponse crear(TramoComisionRequest request) {
        TramoComision tramo = new TramoComision();
        tramo.setDesdeMonto(request.getDesdeMonto());
        tramo.setHastaMonto(request.getHastaMonto());
        tramo.setPorcentaje(request.getPorcentaje());
        tramo.setActivo(true);

        if (request.getEspecialidadId() != null) {
            Especialidad especialidad = especialidadRepository.findById(request.getEspecialidadId())
                    .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con id: " + request.getEspecialidadId()));
            tramo.setEspecialidad(especialidad);
        }

        return mapToResponse(tramoComisionRepository.save(tramo));
    }

    public TramoComisionResponse actualizar(Long id, TramoComisionRequest request) {
        TramoComision tramo = tramoComisionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tramo no encontrado con id: " + id));

        tramo.setDesdeMonto(request.getDesdeMonto());
        tramo.setHastaMonto(request.getHastaMonto());
        tramo.setPorcentaje(request.getPorcentaje());

        if (request.getEspecialidadId() != null) {
            Especialidad especialidad = especialidadRepository.findById(request.getEspecialidadId())
                    .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con id: " + request.getEspecialidadId()));
            tramo.setEspecialidad(especialidad);
        } else {
            tramo.setEspecialidad(null);
        }

        return mapToResponse(tramoComisionRepository.save(tramo));
    }

    public void desactivar(Long id) {
        TramoComision tramo = tramoComisionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tramo no encontrado con id: " + id));
        tramo.setActivo(false);
        tramoComisionRepository.save(tramo);
    }

    private TramoComisionResponse mapToResponse(TramoComision t) {
        TramoComisionResponse res = new TramoComisionResponse();
        res.setId(t.getId());
        res.setDesdeMonto(t.getDesdeMonto());
        res.setHastaMonto(t.getHastaMonto());
        res.setPorcentaje(t.getPorcentaje());
        res.setActivo(t.getActivo());
        if (t.getEspecialidad() != null) {
            res.setEspecialidadId(t.getEspecialidad().getId());
            res.setEspecialidadNombre(t.getEspecialidad().getNombre());
        }
        return res;
    }
}
