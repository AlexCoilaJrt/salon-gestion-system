package pe.com.salon.salongestionapi.menu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.com.salon.salongestionapi.menu.entity.AccesoMain;

@Repository
public interface AccesoMainRepository extends JpaRepository<AccesoMain, Long> {
}
