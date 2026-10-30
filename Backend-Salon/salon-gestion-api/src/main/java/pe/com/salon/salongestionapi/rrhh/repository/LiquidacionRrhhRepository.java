package pe.com.salon.salongestionapi.rrhh.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.com.salon.salongestionapi.rrhh.entity.Liquidacion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository("rrhhLiquidacionRepositoryBean")
public interface LiquidacionRrhhRepository extends JpaRepository<Liquidacion, Long> {
    List<Liquidacion> findByEmpleadoId(Long empleadoId);

    Optional<Liquidacion> findFirstByEmpleadoIdAndFechaInicioAndFechaFin(Long empleadoId, LocalDate fechaInicio, LocalDate fechaFin);
}
