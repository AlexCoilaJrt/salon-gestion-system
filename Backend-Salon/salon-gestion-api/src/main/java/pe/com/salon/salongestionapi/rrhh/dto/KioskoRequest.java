package pe.com.salon.salongestionapi.rrhh.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KioskoRequest {
    @NotBlank(message = "El DNI es obligatorio")
    private String dni;
    
    // Puede ser "ASISTENCIA" (Entrada/Salida) o "DESCANSO" (Inicio/Fin)
    private String accion;
}
