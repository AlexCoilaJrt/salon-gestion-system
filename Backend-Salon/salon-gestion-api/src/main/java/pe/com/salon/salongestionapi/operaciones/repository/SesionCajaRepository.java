package pe.com.salon.salongestionapi.operaciones.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.com.salon.salongestionapi.operaciones.entity.SesionCaja;

import java.util.Optional;

@Repository
public interface SesionCajaRepository extends JpaRepository<SesionCaja, Long> {
    Optional<SesionCaja> findByEstadoTrue(); // Para encontrar la caja abierta actual
}
