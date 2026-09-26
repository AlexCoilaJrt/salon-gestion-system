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

    /**
     * Al levantar la aplicación por primera vez, inserta los valores por defecto
     * si la tabla está vacía. Esto evita que el sistema arranque sin configuración.
     */
    @PostConstruct
    public void inicializarDefaults() {
        insertar(COMISION_DEFAULT_SERVICIOS, "40.00",
                "Porcentaje de comisión por defecto para servicios (cuando no hay regla específica)");
        insertar(COMISION_DEFAULT_PRODUCTOS, "10.00",
                "Porcentaje de comisión por defecto para venta de productos");
        insertar(BONO_FIN_SEMANA, "2.00",
                "Porcentaje de bono adicional los sábados y domingos");
        insertar(BONO_FIN_SEMANA_ACTIVO, "true",
                "Activa o desactiva el bono de fin de semana para especialistas que asistieron ese día (true/false)");
        insertar(PUNTOS_POR_SOL, "1",
                "Puntos de fidelización que se otorgan por cada S/ 1.00 gastado por el cliente");
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


    public List<ConfiguracionSistema> listarTodas() {
        return configuracionRepository.findAll();
    }

    public ConfiguracionSistema actualizar(String clave, String nuevoValor) {
        ConfiguracionSistema config = configuracionRepository.findById(clave)
                .orElseThrow(() -> new RuntimeException("Configuración no encontrada: " + clave));
        config.setValor(nuevoValor);
        return configuracionRepository.save(config);
    }
}
