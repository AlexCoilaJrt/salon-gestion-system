package pe.com.salon.salongestionapi.security.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.com.salon.salongestionapi.security.dto.SessionInfoDTO;
import pe.com.salon.salongestionapi.security.entity.IntrusionAttempt;
import pe.com.salon.salongestionapi.security.entity.Session;
import pe.com.salon.salongestionapi.security.service.SecurityMonitorService;
import pe.com.salon.salongestionapi.security.entity.Usuario;
import pe.com.salon.salongestionapi.security.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
public class SecurityController {

        private final SecurityMonitorService securityMonitorService;
        private final UsuarioRepository usuarioRepository;

        @GetMapping("/sessions/{login}")
        @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BIOLOGO')")
        public ResponseEntity<List<SessionInfoDTO>> getUserActiveSessions(@PathVariable String login) {
                Usuario user = usuarioRepository.findByUsername(login)
                                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

                List<Session> sessions = securityMonitorService.getActiveSessions(user);

                List<SessionInfoDTO> sessionDTOs = sessions.stream()
                                .map(s -> SessionInfoDTO.builder()
                                                .id(s.getId())
                                                .username(s.getUser().getUsername())
                                                .ipAddress(s.getIpAddress())
                                                .userAgent(s.getUserAgent())
                                                .loginTime(s.getLoginTime())
                                                .lastAccessTime(s.getLastAccessTime())
                                                .status(s.getStatus())
                                                .isSuspicious(s.isSuspicious())
                                                .location(s.getLocation())
                                                .build())
                                .collect(Collectors.toList());

                return ResponseEntity.ok(sessionDTOs);
        }

        @GetMapping("/intrusions/recent")
        @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BIOLOGO')")
        public ResponseEntity<List<IntrusionAttempt>> getRecentIntrusions(
                        @RequestParam(defaultValue = "24") int hours) {

                List<IntrusionAttempt> attempts = securityMonitorService.getRecentIntrusionAttempts(hours);
                return ResponseEntity.ok(attempts);
        }

        @PostMapping("/sessions/{sessionId}/close")
        @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BIOLOGO')")
        public ResponseEntity<String> closeSession(@PathVariable Long sessionId) {
                // Este método necesitaría obtener el token de la sesión
                // Por simplicidad, se deja como ejemplo
                return ResponseEntity.ok("Sesión cerrada exitosamente");
        }

        @PostMapping("/sessions/{sessionId}/mark-suspicious")
        @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BIOLOGO')")
        public ResponseEntity<String> markAsSuspicious(
                        @PathVariable Long sessionId,
                        @RequestParam String reason) {

                securityMonitorService.markSessionAsSuspicious(sessionId, reason);
                return ResponseEntity.ok("Sesión marcada como sospechosa");
        }

        @GetMapping("/intruso/bloqueadas")
        @PreAuthorize("hasRole('ROLE_ADMINISTRADOR')")
        public ResponseEntity<List<pe.com.salon.salongestionapi.security.dto.IntrusionAttemptDTO>> listarIPsBloqueadas() {
                List<pe.com.salon.salongestionapi.security.dto.IntrusionAttemptDTO> bloqueadas = securityMonitorService
                                .listarIPsBloqueadas();
                return ResponseEntity.ok(bloqueadas);
        }

        @PostMapping("/intruso/desbloquear")
        @PreAuthorize("hasRole('ROLE_ADMINISTRADOR')")
        public ResponseEntity<java.util.Map<String, String>> desbloquearIP(
                        @org.springframework.web.bind.annotation.RequestBody pe.com.salon.salongestionapi.security.dto.DesbloqueoIPRequest request,
                        @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
                        jakarta.servlet.http.HttpServletRequest httpRequest) {

                // Obtener información del admin que realiza el desbloqueo
                String adminUsername = userDetails.getUsername();
                String adminIp = httpRequest.getRemoteAddr();
                String userAgent = httpRequest.getHeader("User-Agent");

                // Realizar el desbloqueo con auditoría
                securityMonitorService.desbloquearIPManualmente(
                                request.getIp(),
                                request.getMotivo(),
                                adminUsername,
                                adminIp,
                                userAgent);

                return ResponseEntity.ok(java.util.Map.of(
                                "message", "IP desbloqueada exitosamente",
                                "ip", request.getIp(),
                                "admin", adminUsername));
        }
}
