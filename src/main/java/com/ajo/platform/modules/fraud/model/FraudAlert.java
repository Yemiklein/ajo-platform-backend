package com.ajo.platform.modules.fraud.model;

import com.ajo.platform.modules.auth.model.User;
import com.ajo.platform.modules.groups.model.Group;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fraud_alerts")
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = AlertStatus.PENDING;
    }

    public enum AlertType {
        MULTIPLE_DEFAULTS,
        SUSPICIOUS_PATTERN,
        LATE_PAYMENT,
        DUPLICATE_PAYMENT_ATTEMPT,
        ACCOUNT_TAKEOVER_SUSPECTED
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum AlertStatus {
        PENDING, INVESTIGATING, RESOLVED, FALSE_POSITIVE
    }
}