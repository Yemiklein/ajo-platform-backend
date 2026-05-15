package com.ajo.platform.modules.groups.model;

import com.ajo.platform.modules.auth.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "groups")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal contributionAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CycleType cycleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupStatus status;

    @Column(nullable = false)
    private Integer maxMembers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GroupMember> members = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = GroupStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum CycleType {
        DAILY, WEEKLY, MONTHLY
    }

    public enum GroupStatus {
        PENDING, ACTIVE, COMPLETED, CANCELLED
    }
}