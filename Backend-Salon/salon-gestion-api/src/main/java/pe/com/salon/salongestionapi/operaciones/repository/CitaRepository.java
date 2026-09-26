package pe.com.salon.salongestionapi.operaciones.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.com.salon.salongestionapi.operaciones.entity.Cita;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {
}
