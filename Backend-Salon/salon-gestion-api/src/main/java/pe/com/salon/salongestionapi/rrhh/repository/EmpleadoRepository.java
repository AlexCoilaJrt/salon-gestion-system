package pe.com.salon.salongestionapi.rrhh.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.com.salon.salongestionapi.rrhh.entity.Empleado;

import java.util.Optional;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    Optional<Empleado> findByDni(String dni);
    java.util.List<Empleado> findByEstadoTrue();

    @Query(value = "SELECT e FROM Empleado e WHERE " +
           "(:search = '' OR " +
           "LOWER(e.nombres) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(e.apellidos) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(e.dni) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(e.email) LIKE LOWER(CONCAT('%',:search,'%')))",
           countQuery = "SELECT COUNT(e) FROM Empleado e WHERE " +
           "(:search = '' OR " +
           "LOWER(e.nombres) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(e.apellidos) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(e.dni) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(e.email) LIKE LOWER(CONCAT('%',:search,'%')))")
    Page<Empleado> buscarPaginado(@Param("search") String search, Pageable pageable);
}
