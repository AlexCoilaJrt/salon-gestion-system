package pe.com.salon.salongestionapi.catalogo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.salon.salongestionapi.catalogo.dto.CategoriaRequest;
import pe.com.salon.salongestionapi.catalogo.dto.CategoriaResponse;
import pe.com.salon.salongestionapi.catalogo.entity.Categoria;
import pe.com.salon.salongestionapi.catalogo.repository.CategoriaRepository;
import pe.com.salon.salongestionapi.shared.exception.ResourceNotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarActivas() {
        return categoriaRepository.findByEstadoTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarTodas() {
        return categoriaRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        Categoria categoria = Categoria.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .estado(true)
                .build();

        if (request.getPadreId() != null) {
            Categoria padre = categoriaRepository.findById(request.getPadreId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoria padre no encontrada con id: " + request.getPadreId()));
            categoria.setCategoriaPadre(padre);
        }

        Categoria savedCategoria = categoriaRepository.save(categoria);

        if (request.getPadreId() == null && request.getSubcategorias() != null && !request.getSubcategorias().isEmpty()) {
            for (String subName : request.getSubcategorias()) {
                if (subName != null && !subName.trim().isEmpty()) {
                    Categoria sub = Categoria.builder()
                            .nombre(subName.trim())
                            .estado(true)
                            .categoriaPadre(savedCategoria)
                            .build();
                    categoriaRepository.save(sub);
                }
            }
        }

        return mapToResponse(savedCategoria);
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada con id: " + id));
        
        categoria.setNombre(request.getNombre());
        categoria.setDescripcion(request.getDescripcion());
        
        if (request.getPadreId() != null) {
            if (request.getPadreId().equals(id)) {
                throw new IllegalArgumentException("Una categoría no puede ser padre de sí misma");
            }
            Categoria padre = categoriaRepository.findById(request.getPadreId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoria padre no encontrada con id: " + request.getPadreId()));
            categoria.setCategoriaPadre(padre);
        } else {
            categoria.setCategoriaPadre(null);
        }
        
        return mapToResponse(categoriaRepository.save(categoria));
    }

    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada con id: " + id));
        categoria.setEstado(false);
        categoriaRepository.save(categoria);
    }

    private CategoriaResponse mapToResponse(Categoria categoria) {
        CategoriaResponse response = new CategoriaResponse();
        response.setId(categoria.getId());
        response.setNombre(categoria.getNombre());
        response.setDescripcion(categoria.getDescripcion());
        response.setEstado(categoria.getEstado());
        if (categoria.getCategoriaPadre() != null) {
            response.setPadreId(categoria.getCategoriaPadre().getId());
            response.setPadreNombre(categoria.getCategoriaPadre().getNombre());
        }
        return response;
    }
}
