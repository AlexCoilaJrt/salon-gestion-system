package pe.com.salon.salongestionapi.catalogo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.salon.salongestionapi.catalogo.dto.ProductoRequest;
import pe.com.salon.salongestionapi.catalogo.dto.ProductoResponse;
import pe.com.salon.salongestionapi.catalogo.entity.Producto;
import pe.com.salon.salongestionapi.catalogo.repository.ProductoRepository;
import pe.com.salon.salongestionapi.shared.exception.ResourceNotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;

    public List<ProductoResponse> listarTodos() {
        return productoRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ProductoResponse obtenerPorId(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
        return mapToResponse(producto);
    }

    public ProductoResponse crearProducto(ProductoRequest request) {
        Producto producto = new Producto();
        producto.setNombre(request.getNombre());
        producto.setMarca(request.getMarca());
        producto.setPrecioVenta(request.getPrecioVenta());
        producto.setStockActual(request.getStockActual());
        producto.setStockMinimo(request.getStockMinimo());
        producto.setEstado(true);

        Producto guardado = productoRepository.save(producto);
        return mapToResponse(guardado);
    }

    public ProductoResponse actualizarProducto(Long id, ProductoRequest request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));

        producto.setNombre(request.getNombre());
        producto.setMarca(request.getMarca());
        producto.setPrecioVenta(request.getPrecioVenta());
        producto.setStockActual(request.getStockActual());
        producto.setStockMinimo(request.getStockMinimo());

        Producto actualizado = productoRepository.save(producto);
        return mapToResponse(actualizado);
    }

    public void eliminarProducto(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
        
        producto.setEstado(false);
        productoRepository.save(producto);
    }

    private ProductoResponse mapToResponse(Producto producto) {
        ProductoResponse response = new ProductoResponse();
        response.setId(producto.getId());
        response.setNombre(producto.getNombre());
        response.setMarca(producto.getMarca());
        response.setPrecioVenta(producto.getPrecioVenta());
        response.setStockActual(producto.getStockActual());
        response.setStockMinimo(producto.getStockMinimo());
        response.setEstado(producto.getEstado());
        return response;
    }
}
