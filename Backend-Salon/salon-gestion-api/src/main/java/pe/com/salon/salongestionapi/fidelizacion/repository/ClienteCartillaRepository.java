package pe.com.salon.salongestionapi.fidelizacion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.salon.salongestionapi.fidelizacion.entity.ClienteCartilla;

import java.util.List;
import java.util.Optional;

public interface ClienteCartillaRepository extends JpaRepository<ClienteCartilla, Long> {
    List<ClienteCartilla> findByClienteId(Long clienteId);
    Optional<ClienteCartilla> findByClienteIdAndCartillaFidelizacionIdAndCanjeadaFalse(Long clienteId, Long cartillaFidelizacionId);
    List<ClienteCartilla> findByClienteIdAndCompletadaTrueAndCanjeadaFalse(Long clienteId);

    @Query("SELECT cc FROM ClienteCartilla cc " +
           "JOIN FETCH cc.cartillaFidelizacion cf " +
           "LEFT JOIN FETCH cf.servicio " +
           "WHERE cc.cliente.id = :clienteId AND cc.canjeada = false " +
           "ORDER BY cc.completada DESC, cc.fechaUltimaActualizacion DESC")
    List<ClienteCartilla> findActivasByClienteId(@Param("clienteId") Long clienteId);

    @Query("SELECT cc FROM ClienteCartilla cc " +
           "JOIN FETCH cc.cartillaFidelizacion cf " +
           "LEFT JOIN FETCH cf.servicio " +
           "WHERE cc.ticketCompletadoId = :ticketId")
    List<ClienteCartilla> findByTicketCompletadoId(@Param("ticketId") Long ticketId);
}
