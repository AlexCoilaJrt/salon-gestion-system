package pe.com.salon.salongestionapi.operaciones.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SesionCajaResponse {
    private Long id;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private BigDecimal montoInicial;
    private BigDecimal montoEsperado;
    private BigDecimal montoDeclarado;
    private BigDecimal descuadre;
    private String observaciones;
    private Boolean estado;
    private Long usuarioAperturaId;
    private String usuarioAperturaNombre;
}
