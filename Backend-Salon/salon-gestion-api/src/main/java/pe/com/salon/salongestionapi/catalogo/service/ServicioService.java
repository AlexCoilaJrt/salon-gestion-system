package pe.com.salon.salongestionapi.catalogo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.salon.salongestionapi.catalogo.dto.ServicioRequest;
import pe.com.salon.salongestionapi.catalogo.dto.ServicioResponse;
import pe.com.salon.salongestionapi.catalogo.entity.Servicio;
import pe.com.salon.salongestionapi.catalogo.repository.ServicioRepository;
import pe.com.salon.salongestionapi.catalogo.entity.Categoria;
import pe.com.salon.salongestionapi.catalogo.repository.CategoriaRepository;
import pe.com.salon.salongestionapi.rrhh.entity.Especialidad;
import pe.com.salon.salongestionapi.rrhh.repository.EspecialidadRepository;
import pe.com.salon.salongestionapi.catalogo.entity.Producto;
import pe.com.salon.salongestionapi.catalogo.repository.ProductoRepository;
import pe.com.salon.salongestionapi.catalogo.dto.ProductoResponse;
import pe.com.salon.salongestionapi.shared.exception.ResourceNotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServicioService {

    private final ServicioRepository servicioRepository;
    private final EspecialidadRepository especialidadRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

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
        servicio.setComisionPorcentaje(request.getComisionPorcentaje());
        servicio.setCostoMaterial(request.getCostoMaterial() != null ? request.getCostoMaterial() : java.math.BigDecimal.ZERO);
        servicio.setEstado(true);

        Especialidad especialidad = especialidadRepository.findById(request.getEspecialidadRequeridaId())
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con id: " + request.getEspecialidadRequeridaId()));
        servicio.setEspecialidadRequerida(especialidad);

        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada con id: " + request.getCategoriaId()));
        servicio.setCategoria(categoria);

        if (request.getInsumosIds() != null && !request.getInsumosIds().isEmpty()) {
            List<Producto> insumos = productoRepository.findAllById(request.getInsumosIds());
            servicio.setInsumos(insumos);
        }

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
        servicio.setComisionPorcentaje(request.getComisionPorcentaje());
        servicio.setCostoMaterial(request.getCostoMaterial() != null ? request.getCostoMaterial() : java.math.BigDecimal.ZERO);

        if (!servicio.getEspecialidadRequerida().getId().equals(request.getEspecialidadRequeridaId())) {
            Especialidad especialidad = especialidadRepository.findById(request.getEspecialidadRequeridaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con id: " + request.getEspecialidadRequeridaId()));
            servicio.setEspecialidadRequerida(especialidad);
        }

        if (servicio.getCategoria() == null || !servicio.getCategoria().getId().equals(request.getCategoriaId())) {
            Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada con id: " + request.getCategoriaId()));
            servicio.setCategoria(categoria);
        }

        if (request.getInsumosIds() != null) {
            List<Producto> insumos = productoRepository.findAllById(request.getInsumosIds());
            servicio.setInsumos(insumos);
        } else {
            servicio.setInsumos(new java.util.ArrayList<>());
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
        response.setComisionPorcentaje(servicio.getComisionPorcentaje());
        response.setCostoMaterial(servicio.getCostoMaterial());
        response.setEstado(servicio.getEstado());
        if (servicio.getEspecialidadRequerida() != null) {
            response.setEspecialidadRequeridaId(servicio.getEspecialidadRequerida().getId());
            response.setEspecialidadRequeridaNombre(servicio.getEspecialidadRequerida().getNombre());
        }
        if (servicio.getCategoria() != null) {
            response.setCategoriaId(servicio.getCategoria().getId());
            response.setCategoriaNombre(servicio.getCategoria().getNombre());
        }

        if (servicio.getInsumos() != null) {
            response.setInsumos(servicio.getInsumos().stream().map(this::mapProductoToResponse).collect(java.util.stream.Collectors.toList()));
        } else {
            response.setInsumos(new java.util.ArrayList<>());
        }

        return response;
    }

    private ProductoResponse mapProductoToResponse(Producto producto) {
        ProductoResponse res = new ProductoResponse();
        res.setId(producto.getId());
        res.setNombre(producto.getNombre());
        res.setCosto(producto.getCosto());
        res.setPrecioVenta(producto.getPrecioVenta());
        return res;
    }
}
