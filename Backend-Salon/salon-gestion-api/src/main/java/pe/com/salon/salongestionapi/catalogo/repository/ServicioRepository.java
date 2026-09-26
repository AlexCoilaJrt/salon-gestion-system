package pe.com.salon.salongestionapi.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.com.salon.salongestionapi.catalogo.entity.Servicio;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Long> {
}
