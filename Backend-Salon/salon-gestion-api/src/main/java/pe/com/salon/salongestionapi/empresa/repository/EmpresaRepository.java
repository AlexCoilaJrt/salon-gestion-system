package pe.com.salon.salongestionapi.empresa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.salon.salongestionapi.empresa.entity.Empresa;
import java.util.Optional;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    Optional<Empresa> findFirstByActivoTrue();
}
