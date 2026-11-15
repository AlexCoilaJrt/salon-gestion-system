package pe.com.salon.salongestionapi.shared.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.salon.salongestionapi.shared.entity.ConfiguracionSistema;
import pe.com.salon.salongestionapi.shared.repository.ConfiguracionRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfiguracionService {

    private final ConfiguracionRepository configuracionRepository;

    // Claves canónicas del sistema (constantes para no escribir Strings sueltos)
    public static final String COMISION_DEFAULT_SERVICIOS = "COMISION_DEFAULT_SERVICIOS";
    public static final String COMISION_DEFAULT_PRODUCTOS = "COMISION_DEFAULT_PRODUCTOS";
    public static final String BONO_FIN_SEMANA            = "BONO_FIN_SEMANA";
    public static final String BONO_FIN_SEMANA_ACTIVO     = "BONO_FIN_SEMANA_ACTIVO";
    public static final String PUNTOS_POR_SOL             = "PUNTOS_POR_SOL";
    public static final String SESSION_TIMEOUT_MINUTES    = "SESSION_TIMEOUT_MINUTES";

    // Configuración de Impresora Térmica (XP-E260L)
    public static final String IMPRESORA_TICKET_IP             = "IMPRESORA_TICKET_IP";
    public static final String IMPRESORA_TICKET_PUERTO         = "IMPRESORA_TICKET_PUERTO";
    public static final String IMPRESORA_TICKET_AUTO_IMPRIMIR = "IMPRESORA_TICKET_AUTO_IMPRIMIR";
    public static final String IMPRESORA_TICKET_AUTO_CORTE    = "IMPRESORA_TICKET_AUTO_CORTE";
    public static final String IMPRESORA_TICKET_ABRIR_GAVETA   = "IMPRESORA_TICKET_ABRIR_GAVETA";
    public static final String IMPRESORA_TICKET_MENSAJE_PIE   = "IMPRESORA_TICKET_MENSAJE_PIE";
    public static final String IMPRESORA_TICKET_ANCHO_MM      = "IMPRESORA_TICKET_ANCHO_MM";

    // Configuración de Terminal Biométrico (ZKTeco SenseFace 3A)
    public static final String BIOMETRICO_IP                = "BIOMETRICO_IP";
    public static final String BIOMETRICO_PUERTO            = "BIOMETRICO_PUERTO";
    public static final String BIOMETRICO_COMM_KEY          = "BIOMETRICO_COMM_KEY";
    public static final String BIOMETRICO_NOMBRE            = "BIOMETRICO_NOMBRE";
    public static final String BIOMETRICO_AUTO_SYNC         = "BIOMETRICO_AUTO_SYNC";
    public static final String BIOMETRICO_INTERVALO_MINUTOS = "BIOMETRICO_INTERVALO_MINUTOS";
    public static final String BIOMETRICO_ACTIVO            = "BIOMETRICO_ACTIVO";

    /**
     * Al levantar la aplicación por primera vez, inserta los valores por defecto
     * si la tabla está vacía. Esto evita que el sistema arranque sin configuración.
     */
    @PostConstruct
    public void inicializarDefaults() {
        actualizarOInsertarDefault(COMISION_DEFAULT_SERVICIOS, "0.00",
                "Porcentaje de comisión por defecto para servicios (cuando no hay regla específica)");
        insertar(COMISION_DEFAULT_PRODUCTOS, "10.00",
                "Porcentaje de comisión por defecto para venta de productos");
        insertar(BONO_FIN_SEMANA, "2.00",
                "Porcentaje de bono adicional los sábados y domingos");
        insertar(BONO_FIN_SEMANA_ACTIVO, "true",
                "Activa o desactiva el bono de fin de semana para especialistas que asistieron ese día (true/false)");
        insertar(PUNTOS_POR_SOL, "1",
                "Puntos de fidelización que se otorgan por cada S/ 1.00 gastado por el cliente");

        // Defaults para Impresora Térmica
        insertar(IMPRESORA_TICKET_IP, "192.168.123.100",
                "Dirección IP de la impresora térmica de tickets (ej. Xprinter XP-E260L)");
        insertar(IMPRESORA_TICKET_PUERTO, "9100",
                "Puerto TCP RAW de la impresora térmica (por defecto 9100)");
        insertar(IMPRESORA_TICKET_AUTO_IMPRIMIR, "true",
                "Imprimir automáticamente el ticket al registrar el pago en caja (true/false)");
        insertar(IMPRESORA_TICKET_AUTO_CORTE, "true",
                "Ejecutar corte de papel automático al terminar de imprimir (true/false)");
        insertar(IMPRESORA_TICKET_ABRIR_GAVETA, "true",
                "Enviar pulso para abrir el cajón de dinero al cobrar en caja (true/false)");
        insertar(IMPRESORA_TICKET_MENSAJE_PIE, "¡Gracias por su preferencia!",
                "Mensaje de pie de página impreso en el ticket");
        insertar(IMPRESORA_TICKET_ANCHO_MM, "80",
                "Ancho de papel de la impresora en milímetros (80 o 58)");

        // Defaults para Terminal Biométrico (ZKTeco SenseFace 3A)
        insertar(BIOMETRICO_IP, "192.168.1.201",
                "Dirección IP del terminal biométrico ZKTeco SenseFace 3A");
        insertar(BIOMETRICO_PUERTO, "4370",
                "Puerto de comunicación ZKTeco (por defecto 4370)");
        insertar(BIOMETRICO_COMM_KEY, "0",
                "Clave de comunicación (CommKey) configurada en el terminal ZKTeco (0 por defecto)");
        insertar(BIOMETRICO_NOMBRE, "ZKTeco SenseFace 3A",
                "Nombre descriptivo del terminal biométrico de asistencia");
        insertar(BIOMETRICO_AUTO_SYNC, "true",
                "Sincronizar automáticamente las marcaciones del biométrico con la base de datos (true/false)");
        insertar(BIOMETRICO_INTERVALO_MINUTOS, "15",
                "Intervalo en minutos para la sincronización automática de asistencias");
        insertar(BIOMETRICO_ACTIVO, "true",
                "Indica si el terminal biométrico está habilitado para uso en el sistema (true/false)");

        // Timeout de sesión por defecto: 24 horas (1440 minutos)
        configuracionRepository.findById(SESSION_TIMEOUT_MINUTES).ifPresentOrElse(config -> {
            try {
                long val = Long.parseLong(config.getValor());
                if (val <= 60) {
                    config.setValor("1440");
                    configuracionRepository.save(config);
                }
            } catch (Exception e) {
                config.setValor("1440");
                configuracionRepository.save(config);
            }
        }, () -> {
            configuracionRepository.save(new ConfiguracionSistema(SESSION_TIMEOUT_MINUTES, "1440",
                    "Tiempo de expiración de sesión en minutos (por defecto 1440 min = 24 horas)"));
        });
    }

    private void actualizarOInsertarDefault(String clave, String valorDefault, String descripcion) {
        configuracionRepository.findById(clave).ifPresentOrElse(config -> {
            if ("40.00".equals(config.getValor()) || "40".equals(config.getValor())) {
                config.setValor(valorDefault);
                config.setDescripcion(descripcion);
                configuracionRepository.save(config);
            }
        }, () -> {
            configuracionRepository.save(new ConfiguracionSistema(clave, valorDefault, descripcion));
        });
    }

    private void insertar(String clave, String valorDefault, String descripcion) {
        if (!configuracionRepository.existsById(clave)) {
            configuracionRepository.save(new ConfiguracionSistema(clave, valorDefault, descripcion));
        }
    }

    public BigDecimal getBigDecimal(String clave) {
        return configuracionRepository.findById(clave)
                .map(c -> new BigDecimal(c.getValor()))
                .orElseThrow(() -> new RuntimeException("Configuración no encontrada: " + clave));
    }

    public Integer getInteger(String clave) {
        return configuracionRepository.findById(clave)
                .map(c -> Integer.parseInt(c.getValor()))
                .orElseThrow(() -> new RuntimeException("Configuración no encontrada: " + clave));
    }

    public String getString(String clave) {
        return configuracionRepository.findById(clave)
                .map(ConfiguracionSistema::getValor)
                .orElseThrow(() -> new RuntimeException("Configuración no encontrada: " + clave));
    }

    public Boolean getBoolean(String clave) {
        return configuracionRepository.findById(clave)
                .map(c -> Boolean.parseBoolean(c.getValor()))
                .orElse(false);
    }

    public String getStringOrDefault(String clave, String defaultValue) {
        return configuracionRepository.findById(clave)
                .map(ConfiguracionSistema::getValor)
                .filter(v -> !v.isBlank())
                .orElse(defaultValue);
    }

    public Integer getIntegerOrDefault(String clave, Integer defaultValue) {
        return configuracionRepository.findById(clave)
                .map(c -> {
                    try {
                        return Integer.parseInt(c.getValor());
                    } catch (Exception e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    public Boolean getBooleanOrDefault(String clave, Boolean defaultValue) {
        return configuracionRepository.findById(clave)
                .map(c -> Boolean.parseBoolean(c.getValor()))
                .orElse(defaultValue);
    }


    public List<ConfiguracionSistema> listarTodas() {
        return configuracionRepository.findAll();
    }

    public ConfiguracionSistema actualizar(String clave, String nuevoValor) {
        ConfiguracionSistema config = configuracionRepository.findById(clave)
                .orElseGet(() -> new ConfiguracionSistema(clave, nuevoValor, "Configuración: " + clave));
        config.setValor(nuevoValor);
        return configuracionRepository.save(config);
    }

    public ConfiguracionSistema obtenerPorClave(String clave) {
        return configuracionRepository.findById(clave)
                .orElseGet(() -> new ConfiguracionSistema(clave, SESSION_TIMEOUT_MINUTES.equals(clave) ? "1440" : "", ""));
    }

    /**
     * Retorna el timeout global de sesión en minutos.
     * Por defecto 1440 minutos (24 horas) si no hay configuración en BD.
     */
    public Long getGlobalSessionTimeout() {
        return configuracionRepository.findById(SESSION_TIMEOUT_MINUTES)
                .map(c -> {
                    try {
                        long val = Long.parseLong(c.getValor());
                        return val > 0 ? val : 1440L;
                    } catch (NumberFormatException e) {
                        return 1440L;
                    }
                })
                .orElse(1440L);
    }
}
