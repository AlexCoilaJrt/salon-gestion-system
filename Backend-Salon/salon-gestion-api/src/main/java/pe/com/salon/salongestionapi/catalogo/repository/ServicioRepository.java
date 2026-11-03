package pe.com.salon.salongestionapi.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.com.salon.salongestionapi.catalogo.entity.Servicio;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Long> {

    @Query("SELECT COALESCE(MAX(s.orden), 0) FROM Servicio s WHERE s.categoria.id = :categoriaId")
    Integer findMaxOrdenByCategoriaId(@Param("categoriaId") Long categoriaId);

    @Query("SELECT COALESCE(MAX(s.orden), 0) FROM Servicio s")
    Integer findMaxOrden();
}
