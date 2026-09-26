package pe.com.salon.salongestionapi.finanzas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.com.salon.salongestionapi.finanzas.entity.ReglaComision;

import java.util.Optional;

@Repository
public interface ReglaComisionRepository extends JpaRepository<ReglaComision, Long> {
    Optional<ReglaComision> findByEspecialidadIdAndEstadoTrue(Long especialidadId);
}
