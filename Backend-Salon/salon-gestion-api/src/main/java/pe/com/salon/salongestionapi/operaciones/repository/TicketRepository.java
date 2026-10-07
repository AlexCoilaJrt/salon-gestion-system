package pe.com.salon.salongestionapi.operaciones.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.com.salon.salongestionapi.operaciones.entity.Ticket;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    
    @Query("SELECT SUM(t.total) FROM Ticket t WHERE t.fechaEmision >= :inicio AND t.fechaEmision <= :fin")
    BigDecimal sumTotalByFechaEmisionBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    java.util.List<Ticket> findBySesionCajaId(Long sesionCajaId);

    java.util.List<Ticket> findAllByOrderByFechaEmisionDesc();
    
    java.util.List<Ticket> findByEstadoOrderByFechaEmisionDesc(pe.com.salon.salongestionapi.operaciones.entity.EstadoTicket estado);
}
