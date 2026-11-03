package pe.com.salon.salongestionapi.rrhh.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.com.salon.salongestionapi.rrhh.entity.Adelanto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface AdelantoRepository extends JpaRepository<Adelanto, Long> {

    List<Adelanto> findByEmpleadoIdAndFechaBetweenAndEstadoOrderByFechaDesc(
            Long empleadoId, LocalDate inicio, LocalDate fin, String estado);

    List<Adelanto> findByEmpleadoIdAndEstadoOrderByFechaDesc(Long empleadoId, String estado);

    @Query("SELECT COALESCE(SUM(a.monto), 0) FROM Adelanto a WHERE a.empleado.id = :empleadoId AND a.fecha BETWEEN :inicio AND :fin AND a.estado = 'ACTIVO'")
    BigDecimal sumAdelantosEnPeriodo(@Param("empleadoId") Long empleadoId, @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);
}

