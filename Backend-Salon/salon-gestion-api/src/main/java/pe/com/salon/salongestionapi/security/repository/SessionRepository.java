package pe.com.salon.salongestionapi.security.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import pe.com.salon.salongestionapi.security.entity.Session;
import pe.com.salon.salongestionapi.security.entity.SessionStatus;
import pe.com.salon.salongestionapi.security.entity.Usuario;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

        Optional<Session> findBySessionToken(String sessionToken);

        List<Session> findByUserAndStatus(Usuario user, SessionStatus status);

        long countByUserAndStatus(Usuario user, SessionStatus status);

        List<Session> findByUserAndIpAddress(Usuario user, String ipAddress);

        @Query("SELECT s FROM Session s WHERE s.user = :user ORDER BY s.loginTime DESC LIMIT 1")
        Optional<Session> findLastSessionByUser(@Param("user") Usuario user);

        List<Session> findByIsSuspiciousAndLoginTimeBetween(
                        boolean isSuspicious,
                        LocalDateTime startTime,
                        LocalDateTime endTime);

        List<Session> findByIpAddressAndStatus(String ipAddress, SessionStatus status);

        @Query("SELECT COUNT(s) > 0 FROM Session s WHERE s.user = :user " +
                        "AND s.ipAddress != :currentIp " +
                        "AND s.status = 'ACTIVE' " +
                        "AND s.loginTime >= :sinceTime")
        boolean existsRecentSessionFromDifferentIp(
                        @Param("user") Usuario user,
                        @Param("currentIp") String currentIp,
                        @Param("sinceTime") LocalDateTime sinceTime);
}

