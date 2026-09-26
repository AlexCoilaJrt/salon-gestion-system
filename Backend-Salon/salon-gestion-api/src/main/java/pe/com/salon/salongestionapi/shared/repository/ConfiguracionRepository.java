package pe.com.salon.salongestionapi.shared.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.com.salon.salongestionapi.shared.entity.ConfiguracionSistema;

@Repository
public interface ConfiguracionRepository extends JpaRepository<ConfiguracionSistema, String> {
}
