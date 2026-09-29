package pe.com.salon.salongestionapi.rrhh.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.salon.salongestionapi.rrhh.entity.Comision;
import java.util.List;

public interface ComisionRepository extends JpaRepository<Comision, Long> {
    List<Comision> findByEmpleadoId(Long empleadoId);
}
