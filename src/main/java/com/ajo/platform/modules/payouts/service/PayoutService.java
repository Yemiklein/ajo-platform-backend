package com.ajo.platform.modules.payouts.service;

import com.ajo.platform.modules.auth.model.User;
import com.ajo.platform.modules.auth.repository.UserRepository;
import com.ajo.platform.modules.contributions.repository.ContributionRepository;
import com.ajo.platform.modules.groups.model.Group;
import com.ajo.platform.modules.groups.model.GroupMember;
import com.ajo.platform.modules.groups.repository.GroupMemberRepository;
import com.ajo.platform.modules.groups.repository.GroupRepository;
import com.ajo.platform.modules.payouts.dto.PayoutResponse;
import com.ajo.platform.modules.payouts.dto.PayoutSummary;
import com.ajo.platform.modules.payouts.model.Payout;
import com.ajo.platform.modules.payouts.repository.PayoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayoutService {

    private final PayoutRepository payoutRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ContributionRepository contributionRepository;
    private final UserRepository userRepository;

    @Transactional
    public PayoutResponse triggerPayout(Long groupId, Integer cycleNumber, String email) {

        User requestingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        System.out.println("Group creator ID: " + group.getCreatedBy().getId());
        System.out.println("Requesting user ID: " + requestingUser.getId());

        if (!group.getCreatedBy().getId().equals(requestingUser.getId())) {
            throw new RuntimeException("Only the group creator can trigger payouts");
        }

        // Check payout doesn't already exist for this cycle
        if (payoutRepository.existsByGroupIdAndCycleNumber(groupId, cycleNumber)) {
            throw new RuntimeException("Payout already exists for this cycle");
        }

        // Verify all members have paid for this cycle
        int totalMembers = groupMemberRepository.countByGroupId(groupId);
        int paidMembers = contributionRepository.countPaidContributions(groupId, cycleNumber);

        if (paidMembers < totalMembers) {
            throw new RuntimeException("Not all members have paid for cycle "
                    + cycleNumber + ". Paid: " + paidMembers + "/" + totalMembers);
        }

        // Determine recipient based on payout position matching cycle number
        GroupMember recipient = groupMemberRepository.findByGroupId(groupId)
                .stream()
                .filter(m -> m.getPayoutPosition().equals(cycleNumber))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No member found for payout position " + cycleNumber));

        // Calculate payout amount — total contributions for this cycle
        BigDecimal payoutAmount = group.getContributionAmount()
                .multiply(BigDecimal.valueOf(totalMembers));

        Payout payout = Payout.builder()
                .group(group)
                .recipient(recipient.getUser())
                .cycleNumber(cycleNumber)
                .amount(payoutAmount)
                .status(Payout.PayoutStatus.COMPLETED)
                .narration("Ajo payout for " + group.getName()
                        + " - Cycle " + cycleNumber)
                .disbursedAt(LocalDateTime.now())
                .build();

        payoutRepository.save(payout);

        // Check if this was the last cycle - mark group as completed
        if (cycleNumber >= group.getMaxMembers()) {
            group.setStatus(Group.GroupStatus.COMPLETED);
            groupRepository.save(group);
        }

        return mapToResponse(payout);
    }

    public PayoutSummary getCycleSummary(Long groupId, Integer cycleNumber, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, user.getId())) {
            throw new RuntimeException("You are not a member of this group");
        }

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        int totalMembers = groupMemberRepository.countByGroupId(groupId);
        int paidMembers = contributionRepository.countPaidContributions(groupId, cycleNumber);

        GroupMember recipientMember = groupMemberRepository.findByGroupId(groupId)
                .stream()
                .filter(m -> m.getPayoutPosition().equals(cycleNumber))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No member found for position " + cycleNumber));

        BigDecimal payoutAmount = group.getContributionAmount()
                .multiply(BigDecimal.valueOf(totalMembers));

        String status = payoutRepository.findByGroupIdAndCycleNumber(groupId, cycleNumber)
                .map(p -> p.getStatus().name())
                .orElse("PENDING");

        return PayoutSummary.builder()
                .cycleNumber(cycleNumber)
                .recipientName(recipientMember.getUser().getFirstName()
                        + " " + recipientMember.getUser().getLastName())
                .amount(payoutAmount)
                .status(status)
                .allMembersPaid(paidMembers == totalMembers)
                .paidMembersCount(paidMembers)
                .totalMembers(totalMembers)
                .build();
    }

    public List<PayoutResponse> getGroupPayouts(Long groupId, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, user.getId())) {
            throw new RuntimeException("You are not a member of this group");
        }

        return payoutRepository.findByGroupId(groupId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<PayoutResponse> getMyPayouts(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return payoutRepository.findByRecipientId(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private PayoutResponse mapToResponse(Payout payout) {
        return PayoutResponse.builder()
                .id(payout.getId())
                .groupId(payout.getGroup().getId())
                .groupName(payout.getGroup().getName())
                .recipientName(payout.getRecipient().getFirstName()
                        + " " + payout.getRecipient().getLastName())
                .recipientEmail(payout.getRecipient().getEmail())
                .cycleNumber(payout.getCycleNumber())
                .amount(payout.getAmount())
                .status(payout.getStatus())
                .paymentReference(payout.getPaymentReference())
                .narration(payout.getNarration())
                .createdAt(payout.getCreatedAt())
                .disbursedAt(payout.getDisbursedAt())
                .build();
    }
}