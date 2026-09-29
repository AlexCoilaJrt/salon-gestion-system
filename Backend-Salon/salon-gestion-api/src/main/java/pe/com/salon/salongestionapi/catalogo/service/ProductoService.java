package pe.com.salon.salongestionapi.catalogo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.salon.salongestionapi.catalogo.dto.ProductoRequest;
import pe.com.salon.salongestionapi.catalogo.dto.ProductoResponse;
import pe.com.salon.salongestionapi.catalogo.entity.Categoria;
import pe.com.salon.salongestionapi.catalogo.entity.Producto;
import pe.com.salon.salongestionapi.catalogo.repository.CategoriaRepository;
import pe.com.salon.salongestionapi.catalogo.repository.ProductoRepository;
import pe.com.salon.salongestionapi.shared.exception.ResourceNotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

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
        producto.setSku(request.getSku());
        producto.setProveedor(request.getProveedor());
        producto.setImageUrl(request.getImageUrl());
        producto.setPrecioVenta(request.getPrecioVenta());
        producto.setStockActual(request.getStockActual());
        producto.setStockMinimo(request.getStockMinimo());
        producto.setCosto(request.getCosto());
        producto.setUsoInterno(request.getUsoInterno());
        producto.setVentaDirecta(request.getVentaDirecta());
        producto.setEstado(true);

        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada con id: " + request.getCategoriaId()));
        producto.setCategoria(categoria);

        Producto guardado = productoRepository.save(producto);
        return mapToResponse(guardado);
    }

    public ProductoResponse actualizarProducto(Long id, ProductoRequest request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));

        producto.setNombre(request.getNombre());
        producto.setMarca(request.getMarca());
        producto.setSku(request.getSku());
        producto.setProveedor(request.getProveedor());
        producto.setImageUrl(request.getImageUrl());
        producto.setPrecioVenta(request.getPrecioVenta());
        producto.setStockActual(request.getStockActual());
        producto.setStockMinimo(request.getStockMinimo());
        producto.setCosto(request.getCosto());
        producto.setUsoInterno(request.getUsoInterno());
        producto.setVentaDirecta(request.getVentaDirecta());

        if (producto.getCategoria() == null || !producto.getCategoria().getId().equals(request.getCategoriaId())) {
            Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada con id: " + request.getCategoriaId()));
            producto.setCategoria(categoria);
        }

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
        response.setSku(producto.getSku());
        response.setProveedor(producto.getProveedor());
        response.setImageUrl(producto.getImageUrl());
        response.setPrecioVenta(producto.getPrecioVenta());
        response.setStockActual(producto.getStockActual());
        response.setStockMinimo(producto.getStockMinimo());
        response.setEstado(producto.getEstado());
        response.setCosto(producto.getCosto());
        response.setUsoInterno(producto.getUsoInterno());
        response.setVentaDirecta(producto.getVentaDirecta());
        if (producto.getCategoria() != null) {
            response.setCategoriaId(producto.getCategoria().getId());
            response.setCategoriaNombre(producto.getCategoria().getNombre());
        }
        return response;
    }
}
