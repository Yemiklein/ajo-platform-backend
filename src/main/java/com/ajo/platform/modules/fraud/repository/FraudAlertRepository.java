package com.ajo.platform.modules.fraud.repository;

import com.ajo.platform.modules.fraud.model.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    List<FraudAlert> findByUserIdAndStatus(Long userId, FraudAlert.AlertStatus status);

    List<FraudAlert> findByGroupIdAndStatus(Long groupId, FraudAlert.AlertStatus status);

    List<FraudAlert> findAllByOrderByCreatedAtDesc();

    @Query("SELECT COUNT(f) FROM FraudAlert f WHERE f.user.id = :userId " +
            "AND f.alertType = :alertType AND f.createdAt > :since")
    int countRecentAlerts(
            @Param("userId") Long userId,
            @Param("alertType") FraudAlert.AlertType alertType,
            @Param("since") LocalDateTime since
    );

    List<FraudAlert> findByStatusAndRiskLevel(
            FraudAlert.AlertStatus status,
            FraudAlert.RiskLevel riskLevel
    );
}