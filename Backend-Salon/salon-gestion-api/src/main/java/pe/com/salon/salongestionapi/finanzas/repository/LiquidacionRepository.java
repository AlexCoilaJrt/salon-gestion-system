package pe.com.salon.salongestionapi.finanzas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.com.salon.salongestionapi.finanzas.entity.Liquidacion;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public interface LiquidacionRepository extends JpaRepository<Liquidacion, Long> {
    
    @Query("SELECT SUM(l.totalComisiones) FROM Liquidacion l WHERE l.pagado = true AND l.fechaFin >= :inicio AND l.fechaFin <= :fin")
    BigDecimal sumTotalComisionesByPagadoTrueAndFechaFinBetween(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);
}
