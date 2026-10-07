package pe.com.salon.salongestionapi.rrhh.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.com.salon.salongestionapi.rrhh.entity.Liquidacion;

import java.util.List;

@Repository("rrhhLiquidacionRepositoryBean")
public interface LiquidacionRrhhRepository extends JpaRepository<Liquidacion, Long> {
    List<Liquidacion> findByEmpleadoId(Long empleadoId);
}
