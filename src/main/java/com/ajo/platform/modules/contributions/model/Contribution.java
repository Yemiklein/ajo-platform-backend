package com.ajo.platform.modules.contributions.model;

import com.ajo.platform.modules.auth.model.User;
import com.ajo.platform.modules.groups.model.Group;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "contributions",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"group_id", "user_id", "cycle_number"}
        ))
public class Contribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer cycleNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContributionStatus status;

    @Column(unique = true)
    private String idempotencyKey;

    private String paymentReference;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = ContributionStatus.PENDING;
    }

    public enum ContributionStatus {
        PENDING, PAID, FAILED
    }
}