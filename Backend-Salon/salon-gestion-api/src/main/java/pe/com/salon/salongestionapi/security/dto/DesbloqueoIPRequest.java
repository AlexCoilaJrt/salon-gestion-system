package pe.com.salon.salongestionapi.security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DesbloqueoIPRequest {

    private String ip;
    private String motivo;
}

