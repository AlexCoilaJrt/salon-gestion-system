package pe.com.salon.salongestionapi.security.entity;

import java.time.LocalDateTime;

import pe.com.salon.salongestionapi.security.entity.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "sessiones", indexes = {
        @Index(name = "idx_sessiones_user_id", columnList = "user_id"),
        @Index(name = "idx_sessiones_ip", columnList = "ipAddress"),
        @Index(name = "idx_sessiones_status", columnList = "status"),
        @Index(name = "idx_sessiones_login_time", columnList = "loginTime")
})
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Usuario user;

    @Column(name = "session_token", unique = true, nullable = false, columnDefinition = "TEXT")
    private String sessionToken;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;

    @Column(name = "last_access_time")
    private LocalDateTime lastAccessTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(length = 100)
    private String location;

    @Column(name = "is_suspicious", nullable = false)
    private boolean isSuspicious = false;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @PrePersist
    protected void onCreate() {
        if (loginTime == null) {
            loginTime = LocalDateTime.now();
        }
        if (lastAccessTime == null) {
            lastAccessTime = loginTime;
        }
    }

    public Session() {
    }

    public Session(Long id, Usuario user, String sessionToken, String ipAddress, String userAgent,
            LocalDateTime loginTime, LocalDateTime lastAccessTime, SessionStatus status, String location,
            boolean isSuspicious, String remarks) {
        this.id = id;
        this.user = user;
        this.sessionToken = sessionToken;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.loginTime = loginTime;
        this.lastAccessTime = lastAccessTime;
        this.status = status;
        this.location = location;
        this.isSuspicious = isSuspicious;
        this.remarks = remarks;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUser() {
        return user;
    }

    public void setUser(Usuario user) {
        this.user = user;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public LocalDateTime getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(LocalDateTime lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isSuspicious() {
        return isSuspicious;
    }

    public void setSuspicious(boolean suspicious) {
        isSuspicious = suspicious;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public static class SessionBuilder {
        private Long id;
        private Usuario user;
        private String sessionToken;
        private String ipAddress;
        private String userAgent;
        private LocalDateTime loginTime;
        private LocalDateTime lastAccessTime;
        private SessionStatus status = SessionStatus.ACTIVE;
        private String location;
        private boolean isSuspicious = false;
        private String remarks;

        SessionBuilder() {
        }

        public SessionBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public SessionBuilder user(Usuario user) {
            this.user = user;
            return this;
        }

        public SessionBuilder sessionToken(String sessionToken) {
            this.sessionToken = sessionToken;
            return this;
        }

        public SessionBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public SessionBuilder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public SessionBuilder loginTime(LocalDateTime loginTime) {
            this.loginTime = loginTime;
            return this;
        }

        public SessionBuilder lastAccessTime(LocalDateTime lastAccessTime) {
            this.lastAccessTime = lastAccessTime;
            return this;
        }

        public SessionBuilder status(SessionStatus status) {
            this.status = status;
            return this;
        }

        public SessionBuilder location(String location) {
            this.location = location;
            return this;
        }

        public SessionBuilder isSuspicious(boolean isSuspicious) {
            this.isSuspicious = isSuspicious;
            return this;
        }

        public SessionBuilder remarks(String remarks) {
            this.remarks = remarks;
            return this;
        }

        public Session build() {
            return new Session(id, user, sessionToken, ipAddress, userAgent, loginTime, lastAccessTime, status,
                    location, isSuspicious, remarks);
        }
    }

    public static SessionBuilder builder() {
        return new SessionBuilder();
    }
}
