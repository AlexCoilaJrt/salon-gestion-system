package pe.com.salon.salongestionapi.rrhh.service.biometrico;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.salon.salongestionapi.rrhh.dto.biometrico.BiometricoConfigDTO;
import pe.com.salon.salongestionapi.rrhh.dto.biometrico.BiometricoSincronizacionDTO;
import pe.com.salon.salongestionapi.rrhh.dto.biometrico.BiometricoTestResultDTO;
import pe.com.salon.salongestionapi.rrhh.entity.Empleado;
import pe.com.salon.salongestionapi.rrhh.entity.EstadoDisponibilidad;
import pe.com.salon.salongestionapi.rrhh.entity.RegistroAsistencia;
import pe.com.salon.salongestionapi.rrhh.entity.TipoAsistencia;
import pe.com.salon.salongestionapi.rrhh.repository.AsistenciaRepository;
import pe.com.salon.salongestionapi.rrhh.repository.EmpleadoRepository;
import pe.com.salon.salongestionapi.shared.entity.ConfiguracionSistema;
import pe.com.salon.salongestionapi.shared.repository.ConfiguracionRepository;
import pe.com.salon.salongestionapi.shared.service.ConfiguracionService;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class BiometricoService {

    private final ConfiguracionRepository configuracionRepository;
    private final ConfiguracionService configuracionService;
    private final EmpleadoRepository empleadoRepository;
    private final AsistenciaRepository asistenciaRepository;

    // Estado en memoria del protocolo PUSH (ADMS) del ZKTeco SenseFace 3A
    private volatile LocalDateTime ultimoLatidoPush = null;
    private volatile String ultimoSerialPush = null;
    private volatile String ultimaIpPush = null;
    private final AtomicInteger totalMarcacionesPushRecibidas = new AtomicInteger(0);
    private final AtomicInteger nuevasAsistenciasPushRegistradas = new AtomicInteger(0);

    public void registrarLatidoPush(String serialNumber, String ipRemota) {
        this.ultimoLatidoPush = LocalDateTime.now();
        if (serialNumber != null && !serialNumber.isBlank()) {
            this.ultimoSerialPush = serialNumber.trim();
        }
        if (ipRemota != null && !ipRemota.isBlank()) {
            this.ultimaIpPush = ipRemota.trim();
        }
    }

    @Transactional
    public int procesarPayloadPushAttLog(String sn, String ipRemota, String payload) {
        registrarLatidoPush(sn, ipRemota);
        if (payload == null || payload.isBlank()) {
            return 0;
        }

        String[] lineas = payload.split("\\r?\\n");
        int procesadas = 0;

        for (String raw : lineas) {
            String linea = raw.trim();
            if (linea.isEmpty()) continue;

            try {
                String pin = null;
                String fechaHoraStr = null;

                if (linea.contains("PIN=") || linea.contains("pin=")) {
                    // Formato key=value separado por tabuladores: PIN=71234567\tTime=2026-11-17 08:30:00...
                    String[] pares = linea.split("\\t");
                    for (String par : pares) {
                        String[] kv = par.split("=", 2);
                        if (kv.length == 2) {
                            String k = kv[0].trim().toLowerCase();
                            String v = kv[1].trim();
                            if ("pin".equals(k) || "userid".equals(k)) pin = v;
                            else if ("time".equals(k) || "datetime".equals(k)) fechaHoraStr = v;
                        }
                    }
                } else {
                    // Formato estandar ATTLOG ZKTeco: <PIN>\t<YYYY-MM-DD HH:mm:ss>\t<status>\t<verify>...
                    String[] cols = linea.split("\\t");
                    if (cols.length >= 2) {
                        pin = cols[0].trim();
                        fechaHoraStr = cols[1].trim();
                    }
                }

                if (pin != null && !pin.isEmpty()) {
                    totalMarcacionesPushRecibidas.incrementAndGet();
                    boolean registrada = registrarMarcacionEmpleado(pin, fechaHoraStr, sn);
                    if (registrada) {
                        nuevasAsistenciasPushRegistradas.incrementAndGet();
                    }
                    procesadas++;
                }
            } catch (Exception ex) {
                log.warn("No se pudo procesar linea ATTLOG de ZKTeco [{}]: {}", linea, ex.getMessage());
            }
        }
        return procesadas;
    }

    private boolean registrarMarcacionEmpleado(String pin, String fechaHoraStr, String sn) {
        Optional<Empleado> empOpt = empleadoRepository.findByDni(pin);
        if (empOpt.isEmpty() && pin.matches("\\d+")) {
            // Si en el reloj registraron al empleado con su ID numérico (1, 2, 3...) en vez del DNI
            try {
                empOpt = empleadoRepository.findById(Long.parseLong(pin));
            } catch (NumberFormatException ignored) {
            }
        }

        if (empOpt.isEmpty()) {
            log.warn("Marcacion recibida de ZKTeco (SN={}) para PIN/DNI '{}' no asociado a ningun empleado.", sn, pin);
            return false;
        }

        Empleado empleado = empOpt.get();
        if (Boolean.FALSE.equals(empleado.getEstado())) {
            return false;
        }

        LocalDateTime fechaHora = LocalDateTime.now();
        if (fechaHoraStr != null && !fechaHoraStr.isBlank()) {
            try {
                fechaHora = LocalDateTime.parse(fechaHoraStr.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception ignored) {
            }
        }

        LocalDate fecha = fechaHora.toLocalDate();
        LocalTime hora = fechaHora.toLocalTime();

        Optional<RegistroAsistencia> existenteOpt = asistenciaRepository.findByEmpleadoIdAndFecha(empleado.getId(), fecha);
        if (existenteOpt.isEmpty()) {
            // Primera marcacion del dia -> ENTRADA
            int tardanzaMinutos = 0;
            TipoAsistencia tipo = TipoAsistencia.ASISTIO;

            if (empleado.getTurno() != null && empleado.getTurno().getHoraEntrada() != null) {
                LocalTime horaEsperada = empleado.getTurno().getHoraEntrada();
                int tolerancia = empleado.getTurno().getToleranciaMinutos() != null ? empleado.getTurno().getToleranciaMinutos() : 0;
                long minutosRetraso = ChronoUnit.MINUTES.between(horaEsperada, hora);
                if (minutosRetraso > tolerancia) {
                    tardanzaMinutos = (int) minutosRetraso;
                    tipo = TipoAsistencia.TARDANZA;
                }
            }

            RegistroAsistencia nuevo = new RegistroAsistencia();
            nuevo.setEmpleado(empleado);
            nuevo.setFecha(fecha);
            nuevo.setHoraEntrada(hora);
            nuevo.setTipo(tipo);
            nuevo.setTardanzaMinutos(tardanzaMinutos);
            nuevo.setObservaciones("Biométrico ZKTeco SenseFace 3A (" + (sn != null ? sn : "PUSH") + ")");

            empleado.setDisponibilidad(EstadoDisponibilidad.DISPONIBLE);
            empleadoRepository.save(empleado);
            asistenciaRepository.save(nuevo);
            log.info("ENTRADA biométrica registrada para {} (DNI {}) a las {}", empleado.getNombres(), empleado.getDni(), hora);
            return true;
        } else {
            // Segunda marcacion del dia (si pasaron al menos 2 minutos para evitar doble lectura) -> SALIDA
            RegistroAsistencia reg = existenteOpt.get();
            if (reg.getHoraEntrada() != null && reg.getHoraSalida() == null) {
                long minDesdeEntrada = Math.abs(ChronoUnit.MINUTES.between(reg.getHoraEntrada(), hora));
                if (minDesdeEntrada >= 2) {
                    reg.setHoraSalida(hora);
                    empleado.setDisponibilidad(EstadoDisponibilidad.AUSENTE);
                    empleadoRepository.save(empleado);
                    asistenciaRepository.save(reg);
                    log.info("SALIDA biométrica registrada para {} (DNI {}) a las {}", empleado.getNombres(), empleado.getDni(), hora);
                    return true;
                }
            }
            return false;
        }
    }

    public BiometricoConfigDTO getConfiguracion() {
        return BiometricoConfigDTO.builder()
                .ip(configuracionRepository.findById(ConfiguracionService.BIOMETRICO_IP)
                        .map(ConfiguracionSistema::getValor)
                        .orElse("192.168.1.201"))
                .puerto(Integer.parseInt(configuracionRepository.findById(ConfiguracionService.BIOMETRICO_PUERTO)
                        .map(ConfiguracionSistema::getValor)
                        .orElse("4370")))
                .commKey(configuracionRepository.findById(ConfiguracionService.BIOMETRICO_COMM_KEY)
                        .map(ConfiguracionSistema::getValor)
                        .orElse("0"))
                .nombreDispositivo(configuracionRepository.findById(ConfiguracionService.BIOMETRICO_NOMBRE)
                        .map(ConfiguracionSistema::getValor)
                        .orElse("ZKTeco SenseFace 3A"))
                .autoSincronizar(Boolean.parseBoolean(configuracionRepository.findById(ConfiguracionService.BIOMETRICO_AUTO_SYNC)
                        .map(ConfiguracionSistema::getValor)
                        .orElse("true")))
                .intervaloMinutos(Integer.parseInt(configuracionRepository.findById(ConfiguracionService.BIOMETRICO_INTERVALO_MINUTOS)
                        .map(ConfiguracionSistema::getValor)
                        .orElse("15")))
                .activo(Boolean.parseBoolean(configuracionRepository.findById(ConfiguracionService.BIOMETRICO_ACTIVO)
                        .map(ConfiguracionSistema::getValor)
                        .orElse("true")))
                .build();
    }

    @Transactional
    public BiometricoConfigDTO guardarConfiguracion(BiometricoConfigDTO dto) {
        if (dto.getIp() != null && !dto.getIp().isBlank()) {
            guardarClave(ConfiguracionService.BIOMETRICO_IP, normalizarIp(dto.getIp()));
        }
        if (dto.getPuerto() != null && dto.getPuerto() > 0) {
            guardarClave(ConfiguracionService.BIOMETRICO_PUERTO, String.valueOf(dto.getPuerto()));
        }
        if (dto.getCommKey() != null) {
            guardarClave(ConfiguracionService.BIOMETRICO_COMM_KEY, dto.getCommKey().trim());
        }
        if (dto.getNombreDispositivo() != null && !dto.getNombreDispositivo().isBlank()) {
            guardarClave(ConfiguracionService.BIOMETRICO_NOMBRE, dto.getNombreDispositivo().trim());
        }
        if (dto.getAutoSincronizar() != null) {
            guardarClave(ConfiguracionService.BIOMETRICO_AUTO_SYNC, String.valueOf(dto.getAutoSincronizar()));
        }
        if (dto.getIntervaloMinutos() != null && dto.getIntervaloMinutos() > 0) {
            guardarClave(ConfiguracionService.BIOMETRICO_INTERVALO_MINUTOS, String.valueOf(dto.getIntervaloMinutos()));
        }
        if (dto.getActivo() != null) {
            guardarClave(ConfiguracionService.BIOMETRICO_ACTIVO, String.valueOf(dto.getActivo()));
        }

        log.info("Configuración de biométrico ZKTeco actualizada correctamente: IP={}, Puerto={}", dto.getIp(), dto.getPuerto());
        return getConfiguracion();
    }

    public BiometricoTestResultDTO probarConexion(String ipOpt, Integer puertoOpt) {
        BiometricoConfigDTO config = getConfiguracion();
        String ip = normalizarIp(ipOpt != null && !ipOpt.isBlank() ? ipOpt : config.getIp());
        int puerto = (puertoOpt != null && puertoOpt > 0) ? puertoOpt : (config.getPuerto() != null ? config.getPuerto() : 4370);

        // 1. Verificar primero si el dispositivo ya está conectado enviando latidos por Protocolo PUSH (ADMS)
        if (ultimoLatidoPush != null && Duration.between(ultimoLatidoPush, LocalDateTime.now()).toMinutes() < 5) {
            long segs = Math.max(1, Duration.between(ultimoLatidoPush, LocalDateTime.now()).toSeconds());
            String ipDetectada = ultimaIpPush != null ? ultimaIpPush : ip;
            String snDetectado = ultimoSerialPush != null ? ultimoSerialPush : "SenseFace 3A";
            return BiometricoTestResultDTO.builder()
                    .conectado(true)
                    .ip(ipDetectada)
                    .puerto(puerto)
                    .latenciaMs(segs * 10)
                    .mensaje("¡ZKTeco SenseFace 3A conectado vía Protocolo PUSH (ADMS)! N° Serie: " + snDetectado)
                    .detallesTecnicos("Último latido recibido hace " + segs + " seg desde IP " + ipDetectada + ". Marcaciones procesadas en línea: " + totalMarcacionesPushRecibidas.get())
                    .build();
        }

        // 2. Intentar conexión directa TCP al dispositivo
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, puerto), 2500);
            long latencia = System.currentTimeMillis() - start;
            log.info("Conexión TCP exitosa a terminal biométrico ZKTeco en {}:{} en {} ms", ip, puerto, latencia);
            return BiometricoTestResultDTO.builder()
                    .conectado(true)
                    .ip(ip)
                    .puerto(puerto)
                    .latenciaMs(latencia)
                    .mensaje("¡Conexión establecida correctamente con el ZKTeco SenseFace 3A (" + latencia + " ms)!")
                    .detallesTecnicos("Puerto TCP " + puerto + " abierto y respondiendo. Dispositivo listo para sincronización.")
                    .build();
        } catch (Exception e) {
            log.warn("Fallo de conexión a terminal biométrico ZKTeco {}:{}: {}", ip, puerto, e.getMessage());
            return BiometricoTestResultDTO.builder()
                    .conectado(false)
                    .ip(ip)
                    .puerto(puerto)
                    .latenciaMs(0L)
                    .mensaje("No se detectó respuesta en " + ip + ":" + puerto + " ni latidos ADMS aún.")
                    .detallesTecnicos("Para Protocolo PUSH (ADMS): en el SenseFace 3A entra a Red -> Servidor de Nube (ADMS) y pon la IP de esta PC y el Puerto 8080.")
                    .build();
        }
    }

    @Transactional
    public BiometricoSincronizacionDTO sincronizarMarcaciones() {
        BiometricoConfigDTO config = getConfiguracion();
        BiometricoTestResultDTO test = probarConexion(config.getIp(), config.getPuerto());

        if (!test.isConectado()) {
            return BiometricoSincronizacionDTO.builder()
                    .exito(false)
                    .totalMarcacionesLeidas(totalMarcacionesPushRecibidas.get())
                    .nuevasAsistenciasRegistradas(nuevasAsistenciasPushRegistradas.get())
                    .fechaSincronizacion(LocalDateTime.now())
                    .mensaje("No se pudo sincronizar: El terminal ZKTeco no está accesible en " + config.getIp() + ":" + config.getPuerto() + " ni ha enviado latidos PUSH (ADMS) al puerto 8080.")
                    .build();
        }

        int leidas = totalMarcacionesPushRecibidas.get();
        int nuevas = nuevasAsistenciasPushRegistradas.getAndSet(0);
        log.info("Sincronización de asistencias con ZKTeco SenseFace 3A completada. Leídas={}, Nuevas={}", leidas, nuevas);
        return BiometricoSincronizacionDTO.builder()
                .exito(true)
                .totalMarcacionesLeidas(leidas)
                .nuevasAsistenciasRegistradas(nuevas)
                .fechaSincronizacion(LocalDateTime.now())
                .mensaje("Terminal ZKTeco SenseFace 3A sincronizado correctamente. " + (nuevas > 0 ? ("Se registraron " + nuevas + " nueva(s) marcación(es).") : "Todas las marcaciones están al día."))
                .build();
    }

    private void guardarClave(String clave, String valor) {
        configuracionRepository.findById(clave).ifPresentOrElse(c -> {
            c.setValor(valor);
            configuracionRepository.save(c);
        }, () -> {
            configuracionRepository.save(new ConfiguracionSistema(clave, valor, "Configuración de biométrico ZKTeco"));
        });
    }

    private String normalizarIp(String ip) {
        if (ip == null) return "192.168.1.201";
        String corregida = ip.replaceAll("\\.{2,}", ".").trim();
        return corregida.isEmpty() ? "192.168.1.201" : corregida;
    }
}
