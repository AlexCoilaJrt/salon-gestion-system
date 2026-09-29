package pe.com.salon.salongestionapi.operaciones.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.salon.salongestionapi.operaciones.entity.Egreso;
import java.util.List;

public interface EgresoRepository extends JpaRepository<Egreso, Long> {
    List<Egreso> findBySesionCajaIdOrderByFechaHoraDesc(Long sesionCajaId);
}
