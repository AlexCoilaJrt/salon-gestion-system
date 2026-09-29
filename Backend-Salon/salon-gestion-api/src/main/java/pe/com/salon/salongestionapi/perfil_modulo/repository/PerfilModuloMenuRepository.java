package pe.com.salon.salongestionapi.perfil_modulo.repository;

import pe.com.salon.salongestionapi.perfil_modulo.entity.PerfilModuloMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerfilModuloMenuRepository extends JpaRepository<PerfilModuloMenu, Long> {

    List<PerfilModuloMenu> findByPerfilModuloIdAndIsDeletedFalse(Long perfilModuloId);

    @Modifying
    @Query("DELETE FROM PerfilModuloMenu pmm WHERE pmm.perfilModuloId = :perfilModuloId")
    void deleteByPerfilModuloId(@Param("perfilModuloId") Long perfilModuloId);
}
