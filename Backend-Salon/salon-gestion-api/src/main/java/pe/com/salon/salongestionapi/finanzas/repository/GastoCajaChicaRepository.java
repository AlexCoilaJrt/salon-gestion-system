package pe.com.salon.salongestionapi.finanzas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.com.salon.salongestionapi.finanzas.entity.GastoCajaChica;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface GastoCajaChicaRepository extends JpaRepository<GastoCajaChica, Long> {
    
    @Query("SELECT SUM(g.monto) FROM GastoCajaChica g WHERE g.estado = true AND g.fechaGasto >= :inicio AND g.fechaGasto <= :fin")
    BigDecimal sumMontoByEstadoTrueAndFechaGastoBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
