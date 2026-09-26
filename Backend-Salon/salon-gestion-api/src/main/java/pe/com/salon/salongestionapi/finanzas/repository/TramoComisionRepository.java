package pe.com.salon.salongestionapi.finanzas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.com.salon.salongestionapi.finanzas.entity.TramoComision;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface TramoComisionRepository extends JpaRepository<TramoComision, Long> {

    /**
     * Busca el tramo activo que aplique a una especialidad específica
     * dado un monto de ventas acumuladas en el mes.
     * Si hastaMonto es null significa que ese tramo no tiene límite superior.
     */
    @Query("SELECT t FROM TramoComision t " +
           "WHERE t.activo = true " +
           "AND t.especialidad.id = :especialidadId " +
           "AND t.desdeMonto <= :montoAcumulado " +
           "AND (t.hastaMonto IS NULL OR t.hastaMonto >= :montoAcumulado) " +
           "ORDER BY t.desdeMonto DESC")
    Optional<TramoComision> findTramoActivo(
            @Param("especialidadId") Long especialidadId,
            @Param("montoAcumulado") BigDecimal montoAcumulado);

    /**
     * Tramo global (sin especialidad asignada) para cuando no hay tramo específico.
     */
    @Query("SELECT t FROM TramoComision t " +
           "WHERE t.activo = true " +
           "AND t.especialidad IS NULL " +
           "AND t.desdeMonto <= :montoAcumulado " +
           "AND (t.hastaMonto IS NULL OR t.hastaMonto >= :montoAcumulado) " +
           "ORDER BY t.desdeMonto DESC")
    Optional<TramoComision> findTramoGlobalActivo(
            @Param("montoAcumulado") BigDecimal montoAcumulado);

    List<TramoComision> findByEspecialidadIdOrderByDesdeMonto(Long especialidadId);

    List<TramoComision> findByEspecialidadIsNullOrderByDesdeMonto();
}
