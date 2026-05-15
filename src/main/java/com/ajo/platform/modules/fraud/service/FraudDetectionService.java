package com.ajo.platform.modules.fraud.service;

import com.ajo.platform.modules.auth.model.User;
import com.ajo.platform.modules.auth.repository.UserRepository;
import com.ajo.platform.modules.contributions.repository.ContributionRepository;
import com.ajo.platform.modules.fraud.dto.FraudCheckRequest;
import com.ajo.platform.modules.fraud.dto.FraudCheckResponse;
import com.ajo.platform.modules.fraud.model.FraudAlert;
import com.ajo.platform.modules.fraud.repository.FraudAlertRepository;
import com.ajo.platform.modules.groups.model.Group;
import com.ajo.platform.modules.groups.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final FraudAlertRepository fraudAlertRepository;
    private final ContributionRepository contributionRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    @Transactional
    public FraudCheckResponse checkForFraud(FraudCheckRequest request) {

        int riskScore = 0;
        List<String> reasons = new ArrayList<>();

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Rule 1: Check for duplicate payment attempts in last 5 minutes
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        int recentDuplicateAttempts = fraudAlertRepository.countRecentAlerts(
                user.getId(),
                FraudAlert.AlertType.DUPLICATE_PAYMENT_ATTEMPT,
                fiveMinutesAgo
        );

        if (recentDuplicateAttempts > 0) {
            riskScore += 30;
            reasons.add("Multiple payment attempts detected in short time");
        }

        // Rule 2: Check payment history - has user defaulted before
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        int recentDefaults = fraudAlertRepository.countRecentAlerts(
                user.getId(),
                FraudAlert.AlertType.MULTIPLE_DEFAULTS,
                thirtyDaysAgo
        );

        if (recentDefaults > 2) {
            riskScore += 50;
            reasons.add("User has history of defaults");
            createAlert(user, group, FraudAlert.AlertType.MULTIPLE_DEFAULTS,
                    FraudAlert.RiskLevel.HIGH,
                    "User has " + recentDefaults + " defaults in the last 30 days");
        }

        // Rule 3: Check if contribution already exists (idempotency violation attempt)
        boolean alreadyContributed = contributionRepository
                .existsByGroupIdAndUserIdAndCycleNumber(
                        group.getId(), user.getId(), request.getCycleNumber()
                );

        if (alreadyContributed) {
            riskScore += 40;
            reasons.add("Duplicate contribution attempt for same cycle");
            createAlert(user, group, FraudAlert.AlertType.DUPLICATE_PAYMENT_ATTEMPT,
                    FraudAlert.RiskLevel.MEDIUM,
                    "Attempted duplicate payment for cycle " + request.getCycleNumber());
        }

        FraudAlert.RiskLevel riskLevel = calculateRiskLevel(riskScore);
        String recommendation = getRecommendation(riskLevel);

        return FraudCheckResponse.builder()
                .flagged(riskScore > 50)
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .reasons(reasons)
                .recommendation(recommendation)
                .build();
    }

    private void createAlert(User user, Group group, FraudAlert.AlertType type,
                             FraudAlert.RiskLevel level, String description) {
        FraudAlert alert = FraudAlert.builder()
                .user(user)
                .group(group)
                .alertType(type)
                .riskLevel(level)
                .description(description)
                .status(FraudAlert.AlertStatus.PENDING)
                .build();

        fraudAlertRepository.save(alert);
        log.warn("Fraud alert created: {} for user {}", type, user.getEmail());
    }

    private FraudAlert.RiskLevel calculateRiskLevel(int score) {
        if (score >= 80) return FraudAlert.RiskLevel.CRITICAL;
        if (score >= 50) return FraudAlert.RiskLevel.HIGH;
        if (score >= 30) return FraudAlert.RiskLevel.MEDIUM;
        return FraudAlert.RiskLevel.LOW;
    }

    private String getRecommendation(FraudAlert.RiskLevel level) {
        return switch (level) {
            case CRITICAL -> "Block user immediately and require manual review";
            case HIGH -> "Flag for review and notify admin";
            case MEDIUM -> "Monitor closely and log activity";
            case LOW -> "No action required, continue normally";
        };
    }

    public List<FraudAlert> getPendingAlerts() {
        return fraudAlertRepository.findByStatusAndRiskLevel(
                FraudAlert.AlertStatus.PENDING,
                FraudAlert.RiskLevel.HIGH
        );
    }
}