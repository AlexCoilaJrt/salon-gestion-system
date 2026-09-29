package pe.com.salon.salongestionapi.security.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import pe.com.salon.salongestionapi.security.entity.FailureReason;
import pe.com.salon.salongestionapi.security.entity.IntrusionAttempt;
import pe.com.salon.salongestionapi.security.entity.Session;
import pe.com.salon.salongestionapi.security.entity.SessionStatus;
import pe.com.salon.salongestionapi.security.repository.IntrusionAttemptRepository;
import pe.com.salon.salongestionapi.security.repository.SessionRepository;
import pe.com.salon.salongestionapi.security.dto.IntrusionAttemptDTO;
import pe.com.salon.salongestionapi.reports.audit.service.AuditService;
import pe.com.salon.salongestionapi.security.entity.Usuario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SecurityMonitorService {

        private static final Logger log = LoggerFactory.getLogger(SecurityMonitorService.class);

        private final SessionRepository sessionRepository;
        private final IntrusionAttemptRepository intrusionAttemptRepository;
        private final AuditService auditService;

        public SecurityMonitorService(SessionRepository sessionRepository,
                        IntrusionAttemptRepository intrusionAttemptRepository,
                        AuditService auditService) {
                this.sessionRepository = sessionRepository;
                this.intrusionAttemptRepository = intrusionAttemptRepository;
                this.auditService = auditService;
        }

        @Value("${security.max-failed-attempts:5}")
        private int maxFailedAttempts;

        @Value("${security.block-duration-minutes:15}")
        private int blockDurationMinutes;

        @Value("${security.suspicious-ip-change-hours:1}")
        private int suspiciousIpChangeHours;

        @Value("${security.max-concurrent-sessions:3}")
        private int maxConcurrentSessions;

        public boolean isIpBlocked(String ipAddress) {
                LocalDateTime sinceTime = LocalDateTime.now().minusMinutes(blockDurationMinutes);
                long failedAttempts = intrusionAttemptRepository
                                .countByIpAddressAndAttemptTimeAfter(ipAddress, sinceTime);

                boolean isBlocked = failedAttempts >= maxFailedAttempts;

                if (isBlocked) {
                        log.warn("IP bloqueada por intentos excesivos: {} ({} intentos en {} minutos)",
                                        ipAddress, failedAttempts, blockDurationMinutes);
                }

                return isBlocked;
        }

        public boolean hasExcessiveFailedAttempts(String username) {
                LocalDateTime sinceTime = LocalDateTime.now().minusMinutes(blockDurationMinutes);
                long failedAttempts = intrusionAttemptRepository
                                .countByUsernameAndAttemptTimeAfter(username, sinceTime);

                boolean hasExcessive = failedAttempts >= maxFailedAttempts;

                if (hasExcessive) {
                        log.warn("Usuario con intentos excesivos: {} ({} intentos en {} minutos)",
                                        username, failedAttempts, blockDurationMinutes);
                }

                return hasExcessive;
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void registerFailedAttempt(String username, String ipAddress, String userAgent, FailureReason reason) {

                LocalDateTime sinceTime = LocalDateTime.now().minusMinutes(blockDurationMinutes);
                long consecutiveAttempts = intrusionAttemptRepository
                                .countByIpAddressAndAttemptTimeAfter(ipAddress, sinceTime) + 1;

                boolean willCauseBlock = consecutiveAttempts >= maxFailedAttempts;

                IntrusionAttempt attempt = IntrusionAttempt.builder()
                                .username(username)
                                .ipAddress(ipAddress)
                                .userAgent(userAgent)
                                .failureReason(reason)
                                .consecutiveAttempts((int) consecutiveAttempts)
                                .causedBlock(willCauseBlock)
                                .attemptTime(LocalDateTime.now())
                                .build();

                intrusionAttemptRepository.save(attempt);

                log.info("Intento fallido registrado - Usuario: {}, IP: {}, Razon: {}, Intento #{}",
                                username, ipAddress, reason, consecutiveAttempts);

                if (willCauseBlock) {
                        log.error("ALERTA: IP {} bloqueada por {} intentos fallidos", ipAddress, consecutiveAttempts);
                }
        }

        @Transactional
        public Session registerSuccessfulLogin(Usuario user, String ipAddress, String userAgent,
                        String sessionToken) {

                boolean isSuspicious = isSuspiciousLogin(user, ipAddress);

                Session session = Session.builder()
                                .user(user)
                                .sessionToken(sessionToken)
                                .ipAddress(ipAddress)
                                .userAgent(userAgent)
                                .loginTime(LocalDateTime.now())
                                .lastAccessTime(LocalDateTime.now())
                                .status(SessionStatus.ACTIVE)
                                .isSuspicious(isSuspicious)
                                .build();

                if (isSuspicious) {
                        session.setRemarks("Acceso desde IP diferente a la habitual");
                        log.warn("SESION SOSPECHOSA: Usuario {} desde IP {} (User-Agent: {})",
                                        user.getUsername(), ipAddress, userAgent);
                }

                Session savedSession = sessionRepository.save(session);

                cleanFailedAttemptsForUser(user.getUsername(), ipAddress);

                log.info("Sesión registrada - Usuario: {}, IP: {}, Sospechosa: {}",
                                user.getUsername(), ipAddress, isSuspicious);

                return savedSession;
        }

        public boolean isSuspiciousLogin(Usuario user, String currentIp) {

                LocalDateTime recentTime = LocalDateTime.now().minusHours(suspiciousIpChangeHours);

                boolean differentIpRecently = sessionRepository
                                .existsRecentSessionFromDifferentIp(user, currentIp, recentTime);

                if (differentIpRecently) {
                        log.warn("Detectado cambio de IP sospechoso para usuario {}: nueva IP {}",
                                        user.getUsername(), currentIp);
                        return true;
                }

                long activeSessions = sessionRepository.countByUserAndStatus(user, SessionStatus.ACTIVE);

                if (activeSessions >= maxConcurrentSessions) {
                        log.warn("Usuario {} tiene {} sesiones activas (maximo: {})",
                                        user.getUsername(), activeSessions, maxConcurrentSessions);
                        return true;
                }

                return false;
        }

        @Transactional
        public void cleanFailedAttemptsForUser(String username, String ipAddress) {
                LocalDateTime sinceTime = LocalDateTime.now().minusMinutes(blockDurationMinutes);
                List<IntrusionAttempt> attempts = intrusionAttemptRepository
                                .findByUsernameAndAttemptTimeAfterOrderByAttemptTimeDesc(username, sinceTime);

                if (!attempts.isEmpty()) {
                        log.info("Limpiando {} intentos fallidos para usuario {} desde IP {}",
                                        attempts.size(), username, ipAddress);
                }
        }

        @Transactional
        public void updateLastAccess(String sessionToken) {
                sessionRepository.findBySessionToken(sessionToken)
                                .ifPresent(session -> {
                                        session.setLastAccessTime(LocalDateTime.now());
                                        sessionRepository.save(session);
                                });
        }

        @Transactional
        public void closeSession(String sessionToken) {
                sessionRepository.findBySessionToken(sessionToken)
                                .ifPresent(session -> {
                                        session.setStatus(SessionStatus.CLOSED);
                                        session.setLastAccessTime(LocalDateTime.now());
                                        sessionRepository.save(session);
                                        log.info("Sesión cerrada - Token: {}", sessionToken.substring(0, 20) + "...");
                                });
        }

        @Transactional
        public void markSessionAsSuspicious(Long sessionId, String reason) {
                sessionRepository.findById(sessionId)
                                .ifPresent(session -> {
                                        session.setSuspicious(true);
                                        session.setRemarks(reason);
                                        session.setStatus(SessionStatus.SUSPICIOUS);
                                        sessionRepository.save(session);
                                        log.error("ALERTA: Sesion {} marcada como sospechosa: {}", sessionId, reason);
                                });
        }

        public List<Session> getActiveSessions(Usuario user) {
                return sessionRepository.findByUserAndStatus(user, SessionStatus.ACTIVE);
        }

        public List<IntrusionAttempt> getRecentIntrusionAttempts(int hours) {
                LocalDateTime sinceTime = LocalDateTime.now().minusHours(hours);
                return intrusionAttemptRepository
                                .findByIpAddressAndAttemptTimeAfterOrderByAttemptTimeDesc("*", sinceTime);
        }

        public int getRemainingAttempts(String ipAddress) {
                LocalDateTime sinceTime = LocalDateTime.now().minusMinutes(blockDurationMinutes);
                long failedAttempts = intrusionAttemptRepository
                                .countByIpAddressAndAttemptTimeAfter(ipAddress, sinceTime);

                int remaining = maxFailedAttempts - (int) failedAttempts;
                return Math.max(0, remaining);
        }

        public LocalDateTime getUnblockTime(String ipAddress) {
                LocalDateTime sinceTime = LocalDateTime.now().minusMinutes(blockDurationMinutes);

                List<IntrusionAttempt> attempts = intrusionAttemptRepository
                                .findByIpAddressAndAttemptTimeAfterOrderByAttemptTimeDesc(ipAddress, sinceTime);

                if (attempts.isEmpty()) {
                        return null;
                }

                return attempts.get(0).getAttemptTime().plusMinutes(blockDurationMinutes);
        }

        public List<IntrusionAttemptDTO> listarIPsBloqueadas() {
                LocalDateTime sinceTime = LocalDateTime.now().minusMinutes(blockDurationMinutes);

                List<IntrusionAttempt> todosIntentos = intrusionAttemptRepository
                                .findByAttemptTimeAfterOrderByAttemptTimeDesc(sinceTime);

                java.util.Map<String, java.util.List<IntrusionAttempt>> intentosPorIP = todosIntentos.stream()
                                .collect(java.util.stream.Collectors.groupingBy(IntrusionAttempt::getIpAddress));

                return intentosPorIP.entrySet().stream()
                                .filter(entry -> entry.getValue().size() >= maxFailedAttempts)
                                .map(entry -> {
                                        List<IntrusionAttempt> intentos = entry.getValue();
                                        IntrusionAttempt ultimoIntento = intentos.get(0);

                                        return IntrusionAttemptDTO.builder()
                                                        .ipAddress(entry.getKey())
                                                        .username(ultimoIntento.getUsername())
                                                        .totalIntentos(intentos.size())
                                                        .ultimoIntento(ultimoIntento.getAttemptTime())
                                                        .tiempoDesbloqueo(ultimoIntento.getAttemptTime()
                                                                        .plusMinutes(blockDurationMinutes))
                                                        .motivoBloqueo(ultimoIntento.getFailureReason().toString())
                                                        .build();
                                })
                                .collect(java.util.stream.Collectors.toList());
        }

        @Transactional
        public void desbloquearIPManualmente(String ip, String motivo, String adminUsername,
                        String adminIp, String userAgent) {

                if (!isIpBlocked(ip)) {
                        log.warn("Intento de desbloquear IP no bloqueada: {}", ip);
                        throw new RuntimeException("La IP " + ip + " no esta bloqueada actualmente");
                }

                LocalDateTime sinceTime = LocalDateTime.now().minusMinutes(blockDurationMinutes);
                List<IntrusionAttempt> intentos = intrusionAttemptRepository
                                .findByIpAddressAndAttemptTimeAfterOrderByAttemptTimeDesc(ip, sinceTime);

                int cantidadIntentos = intentos.size();
                String usuariosBloqueados = intentos.stream()
                                .map(IntrusionAttempt::getUsername)
                                .distinct()
                                .collect(java.util.stream.Collectors.joining(", "));
                LocalDateTime primerIntento = intentos.isEmpty() ? null
                                : intentos.get(intentos.size() - 1).getAttemptTime();

                intentos.forEach(intrusionAttemptRepository::delete);

                String detalles = String.format(
                                "IP: %s | Admin: %s | Motivo: %s | Intentos eliminados: %d | Usuarios afectados: %s | Primer intento: %s",
                                ip,
                                adminUsername,
                                motivo,
                                cantidadIntentos,
                                usuariosBloqueados,
                                primerIntento);

                auditService.registerAction(
                                "DESBLOQUEO_IP",
                                "SECURITY",
                                adminUsername,
                                detalles
                );

                log.info(" IP {} desbloqueada manualmente por {}. Motivo: {}. Intentos eliminados: {}",
                                ip, adminUsername, motivo, cantidadIntentos);
        }
}
