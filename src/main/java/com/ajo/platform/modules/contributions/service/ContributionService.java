package com.ajo.platform.modules.contributions.service;

import com.ajo.platform.modules.auth.model.User;
import com.ajo.platform.modules.auth.repository.UserRepository;
import com.ajo.platform.modules.contributions.dto.ContributeRequest;
import com.ajo.platform.modules.contributions.dto.ContributionProgress;
import com.ajo.platform.modules.contributions.dto.ContributionResponse;
import com.ajo.platform.modules.contributions.dto.ContributionSummaryResponse;
import com.ajo.platform.modules.contributions.model.Contribution;
import com.ajo.platform.modules.contributions.repository.ContributionRepository;
import com.ajo.platform.modules.fraud.dto.FraudCheckResponse;
import com.ajo.platform.modules.fraud.model.FraudAlert;
import com.ajo.platform.modules.groups.model.Group;
import com.ajo.platform.modules.groups.repository.GroupMemberRepository;
import com.ajo.platform.modules.groups.repository.GroupRepository;
import com.ajo.platform.modules.fraud.service.FraudDetectionService;
import com.ajo.platform.modules.fraud.dto.FraudCheckRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContributionService {

    private final ContributionRepository contributionRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final FraudDetectionService fraudDetectionService;

    @Transactional
    public ContributionResponse contribute(ContributeRequest request, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Auto-generate idempotency key from user + group + cycle
        String idempotencyKey = "contrib-" + user.getId() + "-" + group.getId() + "-" + request.getCycleNumber();

        // Check idempotency key first — if this key exists, return the existing record
        var existing = contributionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return mapToResponse(existing.get());
        }

        // Verify user is a member of this group
        if (!groupMemberRepository.existsByGroupIdAndUserId(group.getId(), user.getId())) {
            throw new RuntimeException("You are not a member of this group");
        }

        // Verify group is active
        if (group.getStatus() != Group.GroupStatus.ACTIVE) {
            throw new RuntimeException("Group is not active");
        }

        // Prevent duplicate contribution for same cycle
        if (contributionRepository.existsByGroupIdAndUserIdAndCycleNumber(
                group.getId(), user.getId(), request.getCycleNumber())) {
            throw new RuntimeException("You have already contributed for this cycle");
        }

        // Fraud check
        FraudCheckRequest fraudRequest = FraudCheckRequest.builder()
                .userId(user.getId())
                .groupId(group.getId())
                .cycleNumber(request.getCycleNumber())
                .action("CONTRIBUTE")
                .build();

        FraudCheckResponse fraudCheck = fraudDetectionService.checkForFraud(fraudRequest);

        if (fraudCheck.isFlagged() && fraudCheck.getRiskLevel() == FraudAlert.RiskLevel.CRITICAL) {
            throw new RuntimeException("Transaction blocked: " + String.join(", ", fraudCheck.getReasons()));
        }

        Contribution contribution = Contribution.builder()
                .group(group)
                .user(user)
                .cycleNumber(request.getCycleNumber())
                .amount(group.getContributionAmount())
                .status(Contribution.ContributionStatus.PAID)
                .idempotencyKey(idempotencyKey)
                .paymentReference(request.getPaymentReference())
                .paidAt(LocalDateTime.now())
                .build();

        contributionRepository.save(contribution);

        return mapToResponse(contribution);
    }

    public List<ContributionResponse> getGroupContributions(Long groupId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, user.getId())) {
            throw new RuntimeException("You are not a member of this group");
        }

        return contributionRepository.findByGroupIdAndUserId(groupId, user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ContributionResponse> getCycleContributions(Long groupId,
                                                            Integer cycleNumber,
                                                            String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, user.getId())) {
            throw new RuntimeException("You are not a member of this group");
        }

        return contributionRepository.findByGroupIdAndCycleNumber(groupId, cycleNumber)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ContributionProgress getCycleProgress(Long groupId, Integer cycleNumber, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, user.getId())) {
            throw new RuntimeException("You are not a member of this group");
        }

        int totalMembers = groupMemberRepository.countByGroupId(groupId);
        int paidCount = contributionRepository.countPaidContributions(groupId, cycleNumber);
        List<ContributionResponse> contributions = contributionRepository
                .findByGroupIdAndCycleNumber(groupId, cycleNumber)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ContributionProgress.builder()
                .cycleNumber(cycleNumber)
                .totalMembers(totalMembers)
                .paidCount(paidCount)
                .pendingCount(totalMembers - paidCount)
                .contributions(contributions)
                .build();
    }

    private ContributionResponse mapToResponse(Contribution contribution) {
        return ContributionResponse.builder()
                .id(contribution.getId())
                .groupId(contribution.getGroup().getId())
                .groupName(contribution.getGroup().getName())
                .memberName(contribution.getUser().getFirstName()
                        + " " + contribution.getUser().getLastName())
                .cycleNumber(contribution.getCycleNumber())
                .amount(contribution.getAmount())
                .status(contribution.getStatus())
                .idempotencyKey(contribution.getIdempotencyKey())
                .paymentReference(contribution.getPaymentReference())
                .createdAt(contribution.getCreatedAt())
                .paidAt(contribution.getPaidAt())
                .build();
    }


    public ContributionSummaryResponse getGroupContributionSummary(Long groupId, String email) {
        // Check if user is a member
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        boolean isMember = groupMemberRepository.existsByGroupIdAndUserId(groupId, user.getId());
        boolean isCreator = group.getCreatedBy().getId().equals(user.getId());
        boolean isAdmin = "ADMIN".equals(user.getRole().name());

        if (!isMember && !isCreator && !isAdmin) {
            throw new RuntimeException("You are not a member of this group");
        }

        int totalMembers = groupMemberRepository.countByGroupId(groupId);
        long totalContributions = contributionRepository.countTotalPaidContributions(groupId);
        BigDecimal totalAmount = contributionRepository.sumPaidContributions(groupId);

        if (totalAmount == null) totalAmount = BigDecimal.ZERO;

        // Calculate current cycle
        int currentCycle = calculateCurrentCycle(group);
        int expectedContributions = totalMembers * currentCycle;
        double collectionRate = expectedContributions > 0
                ? (totalContributions * 100.0) / expectedContributions
                : 0.0;

        return ContributionSummaryResponse.builder()
                .totalContributions(totalContributions)
                .totalAmount(totalAmount)
                .totalMembers(totalMembers)
                .collectionRate(collectionRate)
                .expectedContributions(expectedContributions)
                .groupName(group.getName())
                .currentCycle(currentCycle)
                .build();
    }

    private int calculateCurrentCycle(Group group) {
        java.time.LocalDateTime created = group.getCreatedAt();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        long daysSinceCreation = java.time.Duration.between(created, now).toDays();

        switch (group.getCycleType()) {
            case DAILY:
                return (int) daysSinceCreation + 1;
            case WEEKLY:
                return (int) (daysSinceCreation / 7) + 1;
            case MONTHLY:
                return (int) (daysSinceCreation / 30) + 1;
            default:
                return 1;
        }
    }
}