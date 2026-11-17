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

                if (linea.contains("PIN=") || linea.contains("pin=") || linea.contains("UserID=")) {
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
            log.warn(">>> [ZKTECO ADMS] Marcacion recibida (SN={}) con ID de Usuario='{}' (hora={}), pero no coincide con el DNI ni el ID de ningun empleado en la base de datos.", sn, pin, fechaHoraStr);
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
            log.info(">>> [ZKTECO ADMS] ENTRADA registrada para {} {} (DNI {}) a las {}", empleado.getNombres(), empleado.getApellidos(), empleado.getDni(), hora);
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
                    log.info(">>> [ZKTECO ADMS] SALIDA registrada para {} {} (DNI {}) a las {}", empleado.getNombres(), empleado.getApellidos(), empleado.getDni(), hora);
                    return true;
                }
            }
            return false;
        }
    }

    public BiometricoConfigDTO getConfiguracion() {
        String ipGuardada = configuracionRepository.findById(ConfiguracionService.BIOMETRICO_IP)
                .map(ConfiguracionSistema::getValor)
                .orElse("192.168.100.201");
        if ("192.168.1.201".equals(ipGuardada)) {
            ipGuardada = "192.168.100.201";
        }
        return BiometricoConfigDTO.builder()
                .ip(ipGuardada)
                .puerto(Integer.parseInt(configuracionRepository.findById(ConfiguracionService.BIOMETRICO_PUERTO)
                        .map(ConfiguracionSistema::getValor)
                        .orElse("8080")))
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

        log.info("Configuración de biométrico ZKTeco actualizada: IP={}", dto.getIp());
        return getConfiguracion();
    }

    public BiometricoTestResultDTO probarConexion(String ipOpt, Integer puertoOpt) {
        BiometricoConfigDTO config = getConfiguracion();
        String ip = normalizarIp(ipOpt != null && !ipOpt.isBlank() ? ipOpt : config.getIp());
        if ("192.168.1.201".equals(ip)) {
            ip = "192.168.100.201";
        }
        int puerto = (puertoOpt != null && puertoOpt > 0) ? puertoOpt : 8080;

        // 1. Verificar primero si el dispositivo ya está conectado enviando latidos por Protocolo PUSH (ADMS)
        if (ultimoLatidoPush != null && Duration.between(ultimoLatidoPush, LocalDateTime.now()).toMinutes() < 5) {
            long segs = Math.max(1, Duration.between(ultimoLatidoPush, LocalDateTime.now()).toSeconds());
            String ipDetectada = ultimaIpPush != null ? ultimaIpPush : ip;
            String snDetectado = ultimoSerialPush != null ? ultimoSerialPush : "SenseFace 3A";
            return BiometricoTestResultDTO.builder()
                    .conectado(true)
                    .ip(ipDetectada)
                    .puerto(puerto)
                    .latenciaMs(segs)
                    .mensaje("¡ZKTeco SenseFace 3A conectado vía Protocolo PUSH (ADMS)! N° Serie: " + snDetectado)
                    .detallesTecnicos("Último latido HTTP ADMS recibido hace " + segs + " seg desde IP " + ipDetectada + ". Marcaciones recibidas: " + totalMarcacionesPushRecibidas.get())
                    .build();
        }

        // 2. Verificar alcanzabilidad en red por ICMP Ping (Modo PUSH ADMS - sin usar puerto 4370)
        long pingStart = System.currentTimeMillis();
        if (hacerPingHost(ip)) {
            long latenciaPing = Math.max(1, System.currentTimeMillis() - pingStart);
            guardarClave(ConfiguracionService.BIOMETRICO_IP, ip);
            return BiometricoTestResultDTO.builder()
                    .conectado(true)
                    .ip(ip)
                    .puerto(puerto)
                    .latenciaMs(latenciaPing)
                    .mensaje("¡ZKTeco SenseFace 3A en línea en " + ip + " (" + latenciaPing + " ms)!")
                    .detallesTecnicos("Equipo respondiendo en red local. Si aún no llegan marcaciones, verifica que en el reloj (Servidor de Nube / ADMS) la opción 'Habilitar HTTPS' esté APAGADA (OFF).")
                    .build();
        }

        return BiometricoTestResultDTO.builder()
                .conectado(false)
                .ip(ip)
                .puerto(puerto)
                .latenciaMs(0L)
                .mensaje("No se detectó respuesta desde " + ip + ". Verifica que el SenseFace 3A esté encendido y en la misma red.")
                .detallesTecnicos("Asegúrate de que en Servidor de Nube (ADMS) esté la IP 192.168.100.64, Puerto 8080 y HTTPS APAGADO (OFF).")
                .build();
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
                    .mensaje("El terminal ZKTeco no está accesible en " + config.getIp() + ".")
                    .build();
        }

        int leidas = totalMarcacionesPushRecibidas.get();
        int nuevas = nuevasAsistenciasPushRegistradas.getAndSet(0);
        return BiometricoSincronizacionDTO.builder()
                .exito(true)
                .totalMarcacionesLeidas(leidas)
                .nuevasAsistenciasRegistradas(nuevas)
                .fechaSincronizacion(LocalDateTime.now())
                .mensaje("Terminal ZKTeco SenseFace 3A en línea (Protocolo PUSH ADMS). " + (nuevas > 0 ? ("Se registraron " + nuevas + " nueva(s) marcación(es).") : "Las marcaciones se reciben automáticamente en tiempo real al pasar tarjeta o rostro."))
                .build();
    }

    private boolean hacerPingHost(String ip) {
        try {
            if (java.net.InetAddress.getByName(ip).isReachable(1500)) {
                return true;
            }
        } catch (Exception ignored) {
        }
        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            ProcessBuilder pb = isWindows
                    ? new ProcessBuilder("ping", "-n", "1", "-w", "1500", ip)
                    : new ProcessBuilder("ping", "-c", "1", "-W", "2", ip);
            Process proc = pb.start();
            int exit = proc.waitFor();
            return exit == 0;
        } catch (Exception e) {
            return false;
        }
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
        if (ip == null) return "192.168.100.201";
        String corregida = ip.replaceAll("\\.{2,}", ".").trim();
        return corregida.isEmpty() ? "192.168.100.201" : corregida;
    }
}
