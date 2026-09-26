package pe.com.salon.salongestionapi.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pe.com.salon.salongestionapi.catalogo.entity.Producto;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // GAP 8: Productos cuyo stock actual es menor o igual al stock mínimo
    @Query("SELECT p FROM Producto p WHERE p.estado = true AND p.stockActual <= p.stockMinimo")
    List<Producto> findProductosConStockBajo();
}

