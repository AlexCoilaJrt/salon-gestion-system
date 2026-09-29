package pe.com.salon.salongestionapi.security.dto;

import pe.com.salon.salongestionapi.security.entity.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionInfoDTO {
    private Long id;
    private String username;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime loginTime;
    private LocalDateTime lastAccessTime;
    private SessionStatus status;
    private boolean isSuspicious;
    private String location;
}

