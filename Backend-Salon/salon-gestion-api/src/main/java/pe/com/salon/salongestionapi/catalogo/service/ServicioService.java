package pe.com.salon.salongestionapi.catalogo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
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
        return servicioRepository.findAll(Sort.by(
                Sort.Order.asc("orden").nullsLast(),
                Sort.Order.asc("id")
        )).stream()
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
        servicio.setComisionPorcentaje(request.getComisionPorcentaje() != null ? request.getComisionPorcentaje() : java.math.BigDecimal.ZERO);
        servicio.setCostoMaterial(request.getCostoMaterial() != null ? request.getCostoMaterial() : java.math.BigDecimal.ZERO);
        servicio.setImageUrl(request.getImageUrl());
        servicio.setEstado(true);

        // Si se envía un orden explícito (> 0), se respeta.
        // Si viene null o <= 0, se coloca automáticamente al final de su categoría.
        if (request.getOrden() != null && request.getOrden() > 0) {
            servicio.setOrden(request.getOrden());
        } else {
            Integer maxOrden = (request.getCategoriaId() != null)
                    ? servicioRepository.findMaxOrdenByCategoriaId(request.getCategoriaId())
                    : servicioRepository.findMaxOrden();
            servicio.setOrden((maxOrden != null && maxOrden > 0 ? maxOrden : 0) + 1);
        }

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
        servicio.setComisionPorcentaje(request.getComisionPorcentaje() != null ? request.getComisionPorcentaje() : java.math.BigDecimal.ZERO);
        servicio.setCostoMaterial(request.getCostoMaterial() != null ? request.getCostoMaterial() : java.math.BigDecimal.ZERO);
        if (request.getImageUrl() != null) {
            servicio.setImageUrl(request.getImageUrl());
        }

        // Posición fija al actualizar:
        // 1. Si el usuario envía explícitamente un orden positivo (> 0), se actualiza.
        // 2. Si el servicio no tenía orden (null o <= 0), se calcula uno al final para estabilizarlo.
        // 3. Si se envía null o <= 0, SE MANTIENE EL ORDEN ACTUAL para que NUNCA salte de posición.
        if (request.getOrden() != null && request.getOrden() > 0) {
            servicio.setOrden(request.getOrden());
        } else if (servicio.getOrden() == null || servicio.getOrden() <= 0) {
            Long catId = (request.getCategoriaId() != null) ? request.getCategoriaId()
                    : (servicio.getCategoria() != null ? servicio.getCategoria().getId() : null);
            Integer maxOrden = (catId != null)
                    ? servicioRepository.findMaxOrdenByCategoriaId(catId)
                    : servicioRepository.findMaxOrden();
            servicio.setOrden((maxOrden != null && maxOrden > 0 ? maxOrden : 0) + 1);
        }

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
        response.setImageUrl(servicio.getImageUrl());
        response.setEstado(servicio.getEstado());
        response.setOrden(servicio.getOrden() != null && servicio.getOrden() > 0 ? servicio.getOrden() : (servicio.getId() != null ? servicio.getId().intValue() : 1));
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
