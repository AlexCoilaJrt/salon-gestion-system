package pe.com.salon.salongestionapi.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.salon.salongestionapi.catalogo.entity.Categoria;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByEstadoTrue();
}
