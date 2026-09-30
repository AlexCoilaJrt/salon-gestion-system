package pe.com.salon.salongestionapi.fidelizacion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.salon.salongestionapi.fidelizacion.entity.ClienteCartilla;

import java.util.List;
import java.util.Optional;

public interface ClienteCartillaRepository extends JpaRepository<ClienteCartilla, Long> {
    List<ClienteCartilla> findByClienteId(Long clienteId);
    Optional<ClienteCartilla> findByClienteIdAndCartillaFidelizacionIdAndCanjeadaFalse(Long clienteId, Long cartillaFidelizacionId);
    List<ClienteCartilla> findByClienteIdAndCompletadaTrueAndCanjeadaFalse(Long clienteId);
}
