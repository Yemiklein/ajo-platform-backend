package com.ajo.platform.modules.payouts.model;

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
@Table(name = "payouts",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"group_id", "cycle_number"}
        ))
public class Payout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(nullable = false)
    private Integer cycleNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayoutStatus status;

    private String paymentReference;

    private String narration;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime disbursedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = PayoutStatus.PENDING;
    }

    public enum PayoutStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
}