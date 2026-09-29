package pe.com.salon.salongestionapi.perfil_modulo.repository;

import pe.com.salon.salongestionapi.perfil_modulo.entity.Modulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModuloRepository extends JpaRepository<Modulo, Long> {
}
