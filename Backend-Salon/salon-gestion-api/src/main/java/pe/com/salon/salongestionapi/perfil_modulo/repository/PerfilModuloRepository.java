package pe.com.salon.salongestionapi.perfil_modulo.repository;

import pe.com.salon.salongestionapi.perfil_modulo.entity.PerfilModulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PerfilModuloRepository extends JpaRepository<PerfilModulo, Long> {

    List<PerfilModulo> findByProfileIdAndIsDeletedFalse(Long profileId);

    Optional<PerfilModulo> findByProfileIdAndModuloIdAndIsDeletedFalse(Long profileId, Long moduloId);
}
