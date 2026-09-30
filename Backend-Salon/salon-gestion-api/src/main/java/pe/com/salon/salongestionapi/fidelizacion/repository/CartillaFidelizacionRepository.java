package pe.com.salon.salongestionapi.fidelizacion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.salon.salongestionapi.fidelizacion.entity.CartillaFidelizacion;

import java.util.List;

public interface CartillaFidelizacionRepository extends JpaRepository<CartillaFidelizacion, Long> {
    List<CartillaFidelizacion> findByEstadoTrue();
    CartillaFidelizacion findByServicioIdAndEstadoTrue(Long servicioId);
}
