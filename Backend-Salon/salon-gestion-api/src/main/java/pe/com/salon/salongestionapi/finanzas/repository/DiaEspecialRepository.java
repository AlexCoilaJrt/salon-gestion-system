package pe.com.salon.salongestionapi.finanzas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.com.salon.salongestionapi.finanzas.entity.DiaEspecial;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DiaEspecialRepository extends JpaRepository<DiaEspecial, Long> {
    Optional<DiaEspecial> findByFecha(LocalDate fecha);
}
