package pe.com.salon.salongestionapi.catalogo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.salon.salongestionapi.catalogo.dto.ServicioRequest;
import pe.com.salon.salongestionapi.catalogo.dto.ServicioResponse;
import pe.com.salon.salongestionapi.catalogo.entity.Servicio;
import pe.com.salon.salongestionapi.catalogo.repository.ServicioRepository;
import pe.com.salon.salongestionapi.rrhh.entity.Especialidad;
import pe.com.salon.salongestionapi.rrhh.repository.EspecialidadRepository;
import pe.com.salon.salongestionapi.shared.exception.ResourceNotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServicioService {

    private final ServicioRepository servicioRepository;
    private final EspecialidadRepository especialidadRepository;

    public List<ServicioResponse> listarTodos() {
        return servicioRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ServicioResponse obtenerPorId(Long id) {
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));
        return mapToResponse(servicio);
    }

    public ServicioResponse crearServicio(ServicioRequest request) {
        Servicio servicio = new Servicio();
        servicio.setNombre(request.getNombre());
        servicio.setDescripcion(request.getDescripcion());
        servicio.setPrecioBase(request.getPrecioBase());
        servicio.setDuracionMinutos(request.getDuracionMinutos());
        servicio.setEstado(true);

        Especialidad especialidad = especialidadRepository.findById(request.getEspecialidadRequeridaId())
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con id: " + request.getEspecialidadRequeridaId()));
        servicio.setEspecialidadRequerida(especialidad);

        Servicio guardado = servicioRepository.save(servicio);
        return mapToResponse(guardado);
    }

    public ServicioResponse actualizarServicio(Long id, ServicioRequest request) {
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));

        servicio.setNombre(request.getNombre());
        servicio.setDescripcion(request.getDescripcion());
        servicio.setPrecioBase(request.getPrecioBase());
        servicio.setDuracionMinutos(request.getDuracionMinutos());

        if (!servicio.getEspecialidadRequerida().getId().equals(request.getEspecialidadRequeridaId())) {
            Especialidad especialidad = especialidadRepository.findById(request.getEspecialidadRequeridaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con id: " + request.getEspecialidadRequeridaId()));
            servicio.setEspecialidadRequerida(especialidad);
        }

        Servicio actualizado = servicioRepository.save(servicio);
        return mapToResponse(actualizado);
    }

    public void eliminarServicio(Long id) {
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));
        
        servicio.setEstado(false);
        servicioRepository.save(servicio);
    }

    private ServicioResponse mapToResponse(Servicio servicio) {
        ServicioResponse response = new ServicioResponse();
        response.setId(servicio.getId());
        response.setNombre(servicio.getNombre());
        response.setDescripcion(servicio.getDescripcion());
        response.setPrecioBase(servicio.getPrecioBase());
        response.setDuracionMinutos(servicio.getDuracionMinutos());
        response.setEstado(servicio.getEstado());
        if (servicio.getEspecialidadRequerida() != null) {
            response.setEspecialidadRequeridaNombre(servicio.getEspecialidadRequerida().getNombre());
        }
        return response;
    }
}
