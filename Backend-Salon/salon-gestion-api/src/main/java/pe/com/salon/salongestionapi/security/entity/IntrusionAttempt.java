package pe.com.salon.salongestionapi.security.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "intruso", indexes = {
        @Index(name = "idx_intruso_username", columnList = "username"),
        @Index(name = "idx_intruso_ip", columnList = "ipAddress"),
        @Index(name = "idx_intruso_attempt_time", columnList = "attemptTime")
})
public class IntrusionAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "attempt_time", nullable = false)
    private LocalDateTime attemptTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason", nullable = false, length = 50)
    private FailureReason failureReason;

    @Column(name = "consecutive_attempts")
    private Integer consecutiveAttempts = 1;

    @Column(name = "caused_block")
    private boolean causedBlock = false;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @PrePersist
    protected void onCreate() {
        if (attemptTime == null) {
            attemptTime = LocalDateTime.now();
        }
    }

    public IntrusionAttempt() {
    }

    public IntrusionAttempt(Long id, String username, String ipAddress, String userAgent, LocalDateTime attemptTime,
            FailureReason failureReason, Integer consecutiveAttempts, boolean causedBlock, String remarks) {
        this.id = id;
        this.username = username;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.attemptTime = attemptTime;
        this.failureReason = failureReason;
        this.consecutiveAttempts = consecutiveAttempts;
        this.causedBlock = causedBlock;
        this.remarks = remarks;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public LocalDateTime getAttemptTime() {
        return attemptTime;
    }

    public void setAttemptTime(LocalDateTime attemptTime) {
        this.attemptTime = attemptTime;
    }

    public FailureReason getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(FailureReason failureReason) {
        this.failureReason = failureReason;
    }

    public Integer getConsecutiveAttempts() {
        return consecutiveAttempts;
    }

    public void setConsecutiveAttempts(Integer consecutiveAttempts) {
        this.consecutiveAttempts = consecutiveAttempts;
    }

    public boolean isCausedBlock() {
        return causedBlock;
    }

    public void setCausedBlock(boolean causedBlock) {
        this.causedBlock = causedBlock;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public static class IntrusionAttemptBuilder {
        private Long id;
        private String username;
        private String ipAddress;
        private String userAgent;
        private LocalDateTime attemptTime;
        private FailureReason failureReason;
        private Integer consecutiveAttempts = 1;
        private boolean causedBlock = false;
        private String remarks;

        IntrusionAttemptBuilder() {
        }

        public IntrusionAttemptBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public IntrusionAttemptBuilder username(String username) {
            this.username = username;
            return this;
        }

        public IntrusionAttemptBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public IntrusionAttemptBuilder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public IntrusionAttemptBuilder attemptTime(LocalDateTime attemptTime) {
            this.attemptTime = attemptTime;
            return this;
        }

        public IntrusionAttemptBuilder failureReason(FailureReason failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public IntrusionAttemptBuilder consecutiveAttempts(Integer consecutiveAttempts) {
            this.consecutiveAttempts = consecutiveAttempts;
            return this;
        }

        public IntrusionAttemptBuilder causedBlock(boolean causedBlock) {
            this.causedBlock = causedBlock;
            return this;
        }

        public IntrusionAttemptBuilder remarks(String remarks) {
            this.remarks = remarks;
            return this;
        }

        public IntrusionAttempt build() {
            return new IntrusionAttempt(id, username, ipAddress, userAgent, attemptTime, failureReason,
                    consecutiveAttempts, causedBlock, remarks);
        }
    }

    public static IntrusionAttemptBuilder builder() {
        return new IntrusionAttemptBuilder();
    }
}
