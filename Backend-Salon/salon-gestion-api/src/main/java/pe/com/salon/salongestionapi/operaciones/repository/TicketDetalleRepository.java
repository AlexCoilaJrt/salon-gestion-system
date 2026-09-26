package pe.com.salon.salongestionapi.operaciones.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.com.salon.salongestionapi.operaciones.entity.TicketDetalle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface TicketDetalleRepository extends JpaRepository<TicketDetalle, Long> {

    @Query("SELECT td FROM TicketDetalle td " +
           "JOIN td.ticket t " +
           "WHERE td.empleado.id = :empleadoId " +
           "AND t.fechaEmision >= :fechaInicio " +
           "AND t.fechaEmision <= :fechaFin")
    List<TicketDetalle> findByEmpleadoAndFechaRango(
            @Param("empleadoId") Long empleadoId, 
            @Param("fechaInicio") LocalDateTime fechaInicio, 
            @Param("fechaFin") LocalDateTime fechaFin);

    @Query("SELECT new pe.com.salon.salongestionapi.analitica.dto.RankingEspecialistaDTO(" +
           "e.id, concat(e.nombres, ' ', e.apellidos), COUNT(td), SUM(td.subtotal)) " +
           "FROM TicketDetalle td " +
           "JOIN td.empleado e " +
           "JOIN td.ticket t " +
           "WHERE t.fechaEmision >= :fechaInicio AND t.fechaEmision <= :fechaFin " +
           "GROUP BY e.id, e.nombres, e.apellidos " +
           "ORDER BY SUM(td.subtotal) DESC")
    List<pe.com.salon.salongestionapi.analitica.dto.RankingEspecialistaDTO> getRankingEspecialistas(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);

    // GAP 9: Top servicios más demandados por rango de fecha
    @Query("SELECT new pe.com.salon.salongestionapi.analitica.dto.ServicioDemandaDTO(" +
           "s.id, s.nombre, COUNT(td), SUM(td.subtotal)) " +
           "FROM TicketDetalle td " +
           "JOIN td.servicio s " +
           "JOIN td.ticket t " +
           "WHERE t.fechaEmision >= :fechaInicio AND t.fechaEmision <= :fechaFin " +
           "GROUP BY s.id, s.nombre " +
           "ORDER BY COUNT(td) DESC")
    List<pe.com.salon.salongestionapi.analitica.dto.ServicioDemandaDTO> getServiciosMasDemandados(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);

    // GAP 9: Suma de ingresos únicamente por servicios
    @Query("SELECT COALESCE(SUM(td.subtotal), 0) FROM TicketDetalle td " +
           "JOIN td.ticket t " +
           "WHERE td.servicio IS NOT NULL " +
           "AND t.fechaEmision >= :fechaInicio AND t.fechaEmision <= :fechaFin")
    java.math.BigDecimal sumIngresosPorServicios(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);

    // GAP 9: Suma de ingresos únicamente por productos
    @Query("SELECT COALESCE(SUM(td.subtotal), 0) FROM TicketDetalle td " +
           "JOIN td.ticket t " +
           "WHERE td.producto IS NOT NULL " +
           "AND t.fechaEmision >= :fechaInicio AND t.fechaEmision <= :fechaFin")
    java.math.BigDecimal sumIngresosPorProductos(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);

    /**
     * Suma el total de ventas generadas por un empleado en un rango de fechas.
     * Usado por el MotorComisionService para determinar en qué tramo escalonado se encuentra.
     */
    @Query("SELECT COALESCE(SUM(td.subtotal), 0) FROM TicketDetalle td " +
           "JOIN td.ticket t " +
           "WHERE td.empleado.id = :empleadoId " +
           "AND t.fechaEmision >= :fechaInicio AND t.fechaEmision <= :fechaFin")
    BigDecimal sumVentasEmpleadoEnPeriodo(
            @Param("empleadoId") Long empleadoId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);
}
