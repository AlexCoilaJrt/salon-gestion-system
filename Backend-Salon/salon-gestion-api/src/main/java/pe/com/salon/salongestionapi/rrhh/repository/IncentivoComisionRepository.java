package pe.com.salon.salongestionapi.rrhh.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.salon.salongestionapi.rrhh.entity.IncentivoComision;

import java.time.LocalDateTime;
import java.util.List;

public interface IncentivoComisionRepository extends JpaRepository<IncentivoComision, Long> {
    
    @Query("SELECT i FROM IncentivoComision i WHERE i.estado = true AND :fecha BETWEEN i.fechaInicio AND i.fechaFin")
    List<IncentivoComision> findActiveIncentivos(@Param("fecha") LocalDateTime fecha);
}
