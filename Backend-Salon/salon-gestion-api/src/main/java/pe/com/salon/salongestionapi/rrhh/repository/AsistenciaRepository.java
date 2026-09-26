package pe.com.salon.salongestionapi.rrhh.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.com.salon.salongestionapi.rrhh.entity.RegistroAsistencia;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AsistenciaRepository extends JpaRepository<RegistroAsistencia, Long> {

    List<RegistroAsistencia> findByEmpleadoIdOrderByFechaDesc(Long empleadoId);

    List<RegistroAsistencia> findByFechaBetweenOrderByFechaDesc(LocalDate inicio, LocalDate fin);

    Optional<RegistroAsistencia> findByEmpleadoIdAndFecha(Long empleadoId, LocalDate fecha);
}
