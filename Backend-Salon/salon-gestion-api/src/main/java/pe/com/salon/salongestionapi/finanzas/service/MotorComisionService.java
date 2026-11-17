package pe.com.salon.salongestionapi.finanzas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.salon.salongestionapi.catalogo.entity.Servicio;
import pe.com.salon.salongestionapi.finanzas.entity.DiaEspecial;
import pe.com.salon.salongestionapi.finanzas.entity.ReglaComision;
import pe.com.salon.salongestionapi.finanzas.entity.TramoComision;
import pe.com.salon.salongestionapi.finanzas.repository.DiaEspecialRepository;
import pe.com.salon.salongestionapi.finanzas.repository.ReglaComisionRepository;
import pe.com.salon.salongestionapi.finanzas.repository.TramoComisionRepository;
import pe.com.salon.salongestionapi.operaciones.repository.TicketDetalleRepository;
import pe.com.salon.salongestionapi.rrhh.entity.TipoAsistencia;
import pe.com.salon.salongestionapi.rrhh.repository.AsistenciaRepository;
import pe.com.salon.salongestionapi.shared.service.ConfiguracionService;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Optional;

/**
 * Motor de Cálculo de Comisiones — Jerarquía completa:
 *
 *  Para SERVICIOS (en orden de prioridad):
 *    1. comisionPorcentaje propio del Servicio (override directo)
 *    2. TramoComision activo según la Especialidad del empleado y su volumen mensual (Tiered)
 *    3. TramoComision global (sin especialidad) si existe y aplica al volumen
 *    4. ReglaComision fija por Especialidad
 *    5. Default dinámico → ConfiguracionSistema.COMISION_DEFAULT_SERVICIOS
 *
 *  Para PRODUCTOS:
 *    → ConfiguracionSistema.COMISION_DEFAULT_PRODUCTOS (siempre)
 *
 *  Descuento de material (antes del cálculo):
 *    base_neta = precio_venta - costoMaterial (si aplica)
 *
 *  Bonos adicionales (se suman al porcentaje final):
 *    - Fin de semana → ConfiguracionSistema.BONO_FIN_SEMANA
 *    - Día festivo registrado → DiaEspecial.bonoPorcentaje
 */
@Service
@RequiredArgsConstructor
public class MotorComisionService {

    private final ReglaComisionRepository reglaComisionRepository;
    private final TramoComisionRepository tramoComisionRepository;
    private final DiaEspecialRepository diaEspecialRepository;
    private final TicketDetalleRepository ticketDetalleRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final ConfiguracionService configuracionService;
    private final pe.com.salon.salongestionapi.rrhh.repository.EspecialidadRepository especialidadRepository;
    private final pe.com.salon.salongestionapi.rrhh.repository.EmpleadoRepository empleadoRepository;

    // -------------------------------------------------------------------------
    // Método principal: calcular comisión para una línea de TicketDetalle
    // -------------------------------------------------------------------------

    /**
     * @param servicio       La entidad Servicio (null si el detalle es un Producto)
     * @param empleadoId     ID del empleado que realizó el trabajo
     * @param especialidadId ID de la especialidad del empleado (puede ser null)
     * @param montoDetalle   Precio unitario × cantidad (subtotal del detalle)
     * @param fechaVenta     Fecha en que ocurrió la venta (para bonos)
     */
    public BigDecimal calcularComision(
            Servicio servicio,
            Long empleadoId,
            Long especialidadId,
            BigDecimal montoDetalle,
            LocalDate fechaVenta) {

        boolean esProducto = (servicio == null);

        // 1. Calcular la base neta descontando costo de material (GAP 5)
        BigDecimal baseNeta = calcularBaseNeta(servicio, montoDetalle);

        // 2. Determinar el porcentaje de comisión
        BigDecimal porcentaje;
        if (esProducto) {
            porcentaje = configuracionService.getBigDecimal(ConfiguracionService.COMISION_DEFAULT_PRODUCTOS);
        } else {
            porcentaje = resolverPorcentajeServicio(servicio, empleadoId, especialidadId, fechaVenta);
        }

        // Si el porcentaje base es 0 o no está definido, no comisiona
        if (porcentaje == null || porcentaje.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // 3. Sumar bono por día especial o fin de semana
        //    Solo aplica si el empleado realmente trabajó ese día (verifica RegistroAsistencia)
        BigDecimal bono = resolverBono(fechaVenta, empleadoId);
        BigDecimal porcentajeFinal = porcentaje.add(bono);

        // 4. comision = base_neta × porcentajeFinal / 100
        return baseNeta
                .multiply(porcentajeFinal)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    // -------------------------------------------------------------------------
    // Sobrecarga simplificada para el NominaService (cálculo por periodos)
    // -------------------------------------------------------------------------
    public BigDecimal calcularComision(Long especialidadId, BigDecimal montoVenta) {
        BigDecimal porcentaje = resolverPorcentajePorEspecialidad(especialidadId, montoVenta);
        return montoVenta.multiply(porcentaje)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    // -------------------------------------------------------------------------
    // Lógica interna
    // -------------------------------------------------------------------------

    private BigDecimal calcularBaseNeta(Servicio servicio, BigDecimal montoDetalle) {
        if (servicio == null) return montoDetalle;
        BigDecimal costo = servicio.getCostoMaterial();
        if (costo == null || costo.compareTo(BigDecimal.ZERO) == 0) return montoDetalle;
        BigDecimal base = montoDetalle.subtract(costo);
        return base.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : base;
    }

    /**
     * Jerarquía completa para servicios (5 niveles).
     */
    private BigDecimal resolverPorcentajeServicio(
            Servicio servicio, Long empleadoId, Long especialidadId, LocalDate fechaVenta) {

        // Validar si el Empleado es estrictamente de Sueldo Fijo
        if (empleadoId != null) {
            pe.com.salon.salongestionapi.rrhh.entity.Empleado emp = empleadoRepository.findById(empleadoId).orElse(null);
            if (emp != null) {
                String tipoRem = emp.getTipoRemuneracionEfectivo();
                if ("SOLO_FIJO".equalsIgnoreCase(tipoRem)) {
                    return BigDecimal.ZERO; // Si el empleado es estrictamente de Solo Sueldo Fijo, no comisiona por servicios.
                }
            }
        }

        // Nivel 1: Comisión propia del Servicio (override directo sólo si es mayor a 0)
        if (servicio != null && servicio.getComisionPorcentaje() != null 
                && servicio.getComisionPorcentaje().compareTo(BigDecimal.ZERO) > 0) {
            return servicio.getComisionPorcentaje();
        }

        // Nivel 2: Comisión desde la Especialidad requerida por el servicio
        if (especialidadId != null) {
            pe.com.salon.salongestionapi.rrhh.entity.Especialidad esp = especialidadRepository.findById(especialidadId).orElse(null);
            if (esp != null) {
                // El tipo de remuneración del empleado (SOLO_FIJO se resolvió arriba) es quien decide si comisiona.
                if (esp.getPorcentajeComision() != null && esp.getPorcentajeComision().compareTo(BigDecimal.ZERO) > 0) {
                    return esp.getPorcentajeComision();
                }
            }
        }

        // Nivel 3: Comisión desde las Especialidades asignadas al propio empleado
        if (empleadoId != null) {
            pe.com.salon.salongestionapi.rrhh.entity.Empleado emp = empleadoRepository.findById(empleadoId).orElse(null);
            if (emp != null && emp.getEspecialidades() != null) {
                for (pe.com.salon.salongestionapi.rrhh.entity.Especialidad espEmp : emp.getEspecialidades()) {
                    if (espEmp.getPorcentajeComision() != null && espEmp.getPorcentajeComision().compareTo(BigDecimal.ZERO) > 0) {
                        return espEmp.getPorcentajeComision();
                    }
                }
            }
        }

        // Calcular monto acumulado del empleado en el mes actual (para tramos)
        BigDecimal acumuladoMes = calcularAcumuladoMes(empleadoId, fechaVenta);

        // Nivel 4: TramoComision específico por Especialidad
        if (especialidadId != null) {
            Optional<TramoComision> tramo = tramoComisionRepository
                    .findTramoActivo(especialidadId, acumuladoMes);
            if (tramo.isPresent()) return tramo.get().getPorcentaje();
        }

        // Nivel 5: TramoComision global (sin especialidad)
        Optional<TramoComision> tramoGlobal = tramoComisionRepository
                .findTramoGlobalActivo(acumuladoMes);
        if (tramoGlobal.isPresent()) return tramoGlobal.get().getPorcentaje();

        // Nivel 6: ReglaComision fija por Especialidad
        if (especialidadId != null) {
            Optional<ReglaComision> regla = reglaComisionRepository
                    .findByEspecialidadIdAndEstadoTrue(especialidadId);
            if (regla.isPresent()) return regla.get().getPorcentaje();
        }

        // Nivel 7: Default dinámico desde la configuración
        BigDecimal defaultServicios = configuracionService.getBigDecimal(ConfiguracionService.COMISION_DEFAULT_SERVICIOS);
        if (defaultServicios != null && defaultServicios.compareTo(BigDecimal.ZERO) > 0) {
            return defaultServicios;
        }

        return BigDecimal.ZERO;
    }

    /**
     * Para el NominaService: solo usa la especialidad y el monto (sin contexto de servicio).
     */
    private BigDecimal resolverPorcentajePorEspecialidad(Long especialidadId, BigDecimal monto) {
        if (especialidadId != null) {
            Optional<TramoComision> tramo = tramoComisionRepository
                    .findTramoActivo(especialidadId, monto);
            if (tramo.isPresent()) return tramo.get().getPorcentaje();

            Optional<ReglaComision> regla = reglaComisionRepository
                    .findByEspecialidadIdAndEstadoTrue(especialidadId);
            if (regla.isPresent()) return regla.get().getPorcentaje();
        }
        return configuracionService.getBigDecimal(ConfiguracionService.COMISION_DEFAULT_SERVICIOS);
    }

    /**
     * Calcula cuánto ha vendido el empleado en el mes de la fecha dada.
     * Retorna 0 si no hay ventas previas.
     */
    private BigDecimal calcularAcumuladoMes(Long empleadoId, LocalDate fecha) {
        if (empleadoId == null || fecha == null) return BigDecimal.ZERO;
        YearMonth mes = YearMonth.from(fecha);
        LocalDateTime inicio = mes.atDay(1).atStartOfDay();
        LocalDateTime fin = mes.atEndOfMonth().atTime(LocalTime.MAX);
        BigDecimal acumulado = ticketDetalleRepository
                .sumVentasEmpleadoEnPeriodo(empleadoId, inicio, fin);
        return acumulado != null ? acumulado : BigDecimal.ZERO;
    }

    /**
     * Determina el bono adicional según el día de la semana o si es festivo.
     *
     * Regla completa para el bono de fin de semana:
     *   1. La feature debe estar activa: BONO_FIN_SEMANA_ACTIVO = "true"
     *   2. Debe ser sábado o domingo
     *   3. El empleado debe tener un RegistroAsistencia para ese día (ASISTIO o TARDANZA)
     *      Es decir, solo los que fueron a trabajar ese día reciben el bono.
     */
    private BigDecimal resolverBono(LocalDate fechaVenta, Long empleadoId) {
        if (fechaVenta == null) return BigDecimal.ZERO;

        DayOfWeek dia = fechaVenta.getDayOfWeek();
        boolean esFindeSemana = (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY);

        if (esFindeSemana) {
            // Verificar que el bono esté activo desde la configuración (el botón del admin)
            boolean bonoActivo = configuracionService.getBoolean(ConfiguracionService.BONO_FIN_SEMANA_ACTIVO);
            if (bonoActivo && empleadoId != null) {
                // Solo aplicar si el empleado realmente trabajó ese día
                boolean trabajoEseDia = asistenciaRepository
                        .findByEmpleadoIdAndFecha(empleadoId, fechaVenta)
                        .map(reg -> reg.getTipo() == TipoAsistencia.ASISTIO
                                 || reg.getTipo() == TipoAsistencia.TARDANZA)
                        .orElse(false);

                if (trabajoEseDia) {
                    return configuracionService.getBigDecimal(ConfiguracionService.BONO_FIN_SEMANA);
                }
            }
            return BigDecimal.ZERO;
        }

        // Verificar si es un día festivo registrado (también requiere que el empleado haya asistido)
        Optional<DiaEspecial> diaEspecial = diaEspecialRepository.findByFecha(fechaVenta);
        if (diaEspecial.isPresent() && empleadoId != null) {
            boolean trabajoEseDia = asistenciaRepository
                    .findByEmpleadoIdAndFecha(empleadoId, fechaVenta)
                    .map(reg -> reg.getTipo() == TipoAsistencia.ASISTIO
                             || reg.getTipo() == TipoAsistencia.TARDANZA)
                    .orElse(false);
            if (trabajoEseDia) {
                return diaEspecial.get().getBonoPorcentaje();
            }
        }

        return BigDecimal.ZERO;
    }
}
