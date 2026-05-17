package com.ajo.platform.modules.groups.service;

import com.ajo.platform.modules.auth.model.User;
import com.ajo.platform.modules.auth.repository.UserRepository;
import com.ajo.platform.modules.groups.dto.*;
import com.ajo.platform.modules.groups.model.Group;
import com.ajo.platform.modules.groups.model.GroupInvite;
import com.ajo.platform.modules.groups.model.GroupMember;
import com.ajo.platform.modules.groups.repository.GroupInviteRepository;
import com.ajo.platform.modules.groups.repository.GroupMemberRepository;
import com.ajo.platform.modules.groups.repository.GroupRepository;
import com.ajo.platform.modules.contributions.repository.ContributionRepository;
import com.ajo.platform.modules.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final GroupInviteRepository groupInviteRepository;
    private final ContributionRepository contributionRepository;
    private final NotificationService notificationService;
    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (groupRepository.existsByNameAndCreatedById(request.getName(), user.getId())) {
            throw new RuntimeException("You already have a group with this name");
        }

        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .contributionAmount(request.getContributionAmount())
                .cycleType(request.getCycleType())
                .maxMembers(request.getMaxMembers())
                .status(Group.GroupStatus.PENDING)
                .createdBy(user)
                .build();

        groupRepository.save(group);

        GroupMember creatorMember = GroupMember.builder()
                .group(group)
                .user(user)
                .payoutPosition(1)
                .status(GroupMember.MemberStatus.ACTIVE)
                .build();

        groupMemberRepository.save(creatorMember);

        return mapToResponse(group, 1);
    }

    @Transactional
    public GroupResponse joinGroup(Long groupId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (group.getStatus() != Group.GroupStatus.PENDING) {
            throw new RuntimeException("Group is no longer accepting members");
        }

        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, user.getId())) {
            throw new RuntimeException("You are already a member of this group");
        }

        int currentMembers = groupMemberRepository.countByGroupId(groupId);

        if (currentMembers >= group.getMaxMembers()) {
            throw new RuntimeException("Group is full");
        }

        GroupMember member = GroupMember.builder()
                .group(group)
                .user(user)
                .payoutPosition(currentMembers + 1)
                .status(GroupMember.MemberStatus.ACTIVE)
                .build();

        groupMemberRepository.save(member);

        if (currentMembers + 1 >= group.getMaxMembers()) {
            group.setStatus(Group.GroupStatus.ACTIVE);
            groupRepository.save(group);
        }

        return mapToResponse(group, currentMembers + 1);
    }

    public List<GroupResponse> getMyGroups(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return groupRepository.findGroupsByMemberId(user.getId())
                .stream()
                .map(group -> mapToResponse(group,
                        groupMemberRepository.countByGroupId(group.getId())))
                .collect(Collectors.toList());
    }

    public GroupResponse getGroup(Long groupId, String email) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        int memberCount = groupMemberRepository.countByGroupId(groupId);
        return mapToResponse(group, memberCount);
    }

    // ============ NEW FEATURE 1: GROUP INVITE SYSTEM ============

    @Transactional
    public InviteResponse inviteMember(Long groupId, InviteMemberRequest request, String userEmail) {
        User invitedBy = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Check if user is the group creator or admin
        if (!group.getCreatedBy().getId().equals(invitedBy.getId())) {
            throw new RuntimeException("Only group creator can invite members");
        }

        // Check if group is still active/accepting members
        if (group.getStatus() == Group.GroupStatus.COMPLETED || group.getStatus() == Group.GroupStatus.CANCELLED) {
            throw new RuntimeException("Group is no longer active");
        }

        // Check if member limit is reached
        int currentMembers = groupMemberRepository.countByGroupId(groupId);
        if (currentMembers >= group.getMaxMembers()) {
            throw new RuntimeException("Group has reached maximum member limit");
        }

        // Check if user is already a member
        User invitedUser = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (invitedUser != null && groupMemberRepository.existsByGroupIdAndUserId(groupId, invitedUser.getId())) {
            throw new RuntimeException("User is already a member of this group");
        }

        // Remove any existing pending invites for this email and group
        groupInviteRepository.deleteByGroupIdAndInvitedEmail(groupId, request.getEmail());

        // Create new invite
        String inviteCode = UUID.randomUUID().toString();
        String inviteLink = "https://ajo-platform-frontend.vercel.app/join/" + inviteCode;

        // For local development, use localhost
        if (System.getProperty("spring.profiles.active") == null ||
                System.getProperty("spring.profiles.active").equals("local")) {
            inviteLink = "http://localhost:3000/join/" + inviteCode;
        }

        GroupInvite invite = GroupInvite.builder()
                .inviteCode(inviteCode)
                .group(group)
                .invitedBy(invitedBy)
                .invitedEmail(request.getEmail())
                .status(GroupInvite.InviteStatus.PENDING)
                .build();

        groupInviteRepository.save(invite);

        // TODO: Send email notification with invite link
        // notificationService.sendInviteEmail(request.getEmail(), group.getName(), inviteLink, request.getMessage());

        return InviteResponse.builder()
                .inviteId(invite.getId())
                .inviteCode(inviteCode)
                .inviteLink(inviteLink)
                .message(request.getMessage())
                .expiresAt(invite.getExpiresAt().toString())
                .build();
    }

    @Transactional
    public GroupResponse joinViaInvite(String inviteCode, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        GroupInvite invite = groupInviteRepository.findByInviteCodeAndStatus(inviteCode, GroupInvite.InviteStatus.PENDING)
                .orElseThrow(() -> new RuntimeException("Invalid or expired invite link"));

        // Check if invite is expired
        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            invite.setStatus(GroupInvite.InviteStatus.EXPIRED);
            groupInviteRepository.save(invite);
            throw new RuntimeException("Invite link has expired");
        }

        Group group = invite.getGroup();

        // Check if group is still active
        if (group.getStatus() == Group.GroupStatus.COMPLETED || group.getStatus() == Group.GroupStatus.CANCELLED) {
            throw new RuntimeException("Group is no longer active");
        }

        // Check if member limit is reached
        int currentMembers = groupMemberRepository.countByGroupId(group.getId());
        if (currentMembers >= group.getMaxMembers()) {
            invite.setStatus(GroupInvite.InviteStatus.EXPIRED);
            groupInviteRepository.save(invite);
            throw new RuntimeException("Group has reached maximum member limit");
        }

        // Check if user is already a member
        if (groupMemberRepository.existsByGroupIdAndUserId(group.getId(), user.getId())) {
            throw new RuntimeException("You are already a member of this group");
        }

        // Add user to group
        GroupMember member = GroupMember.builder()
                .group(group)
                .user(user)
                .payoutPosition(currentMembers + 1)
                .status(GroupMember.MemberStatus.ACTIVE)
                .build();

        groupMemberRepository.save(member);

        // Mark invite as accepted
        invite.setStatus(GroupInvite.InviteStatus.ACCEPTED);
        invite.setAcceptedAt(LocalDateTime.now());
        groupInviteRepository.save(invite);

        // Activate group if member limit reached
        if (currentMembers + 1 >= group.getMaxMembers()) {
            group.setStatus(Group.GroupStatus.ACTIVE);
            groupRepository.save(group);
        }

        return mapToResponse(group, currentMembers + 1);
    }

    // ============ NEW FEATURE 2: MEMBER CONTRIBUTION TRACKING ============

    public GroupContributionSummaryDto getContributionSummary(Long groupId, String userEmail) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Verify user is a member
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, user.getId())) {
            throw new RuntimeException("You are not a member of this group");
        }

        List<GroupMember> members = groupMemberRepository.findByGroupIdAndStatus(groupId, "ACTIVE");

        BigDecimal totalExpectedAmount = group.getContributionAmount()
                .multiply(BigDecimal.valueOf(members.size()));

        BigDecimal totalReceivedAmount = contributionRepository.sumAmountByGroupIdAndStatus(groupId, "PAID")
                .orElse(BigDecimal.ZERO);

        BigDecimal collectionRate = totalExpectedAmount.compareTo(BigDecimal.ZERO) > 0
                ? totalReceivedAmount.multiply(BigDecimal.valueOf(100))
                .divide(totalExpectedAmount, 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<MemberContributionDto> memberContributions = members.stream()
                .map(member -> getMemberContributionSummary(groupId, member.getUser().getId()))
                .collect(Collectors.toList());

        return GroupContributionSummaryDto.builder()
                .groupId(groupId)
                .groupName(group.getName())
                .totalMembers(members.size())
                .currentCycle(calculateCurrentCycle(group))
                .totalExpectedAmount(totalExpectedAmount)
                .totalReceivedAmount(totalReceivedAmount)
                .collectionRate(collectionRate)
                .memberContributions(memberContributions)
                .build();
    }

    public MemberContributionDto getMemberContributions(Long groupId, Long memberId, String userEmail) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Verify requesting user is a member or admin
        User requestingUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, requestingUser.getId())
                && !"ADMIN".equals(requestingUser.getRole().name())) {
            throw new RuntimeException("You are not authorized to view this information");
        }

        return getMemberContributionSummary(groupId, memberId);
    }

    private MemberContributionDto getMemberContributionSummary(Long groupId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        int cyclesPaid = contributionRepository.countByGroupIdAndUserIdAndStatus(groupId, userId, "PAID");
        int cyclesMissed = calculateCurrentCycle(group) - cyclesPaid;

        BigDecimal totalContributed = contributionRepository.sumAmountByGroupIdAndUserIdAndStatus(groupId, userId, "PAID")
                .orElse(BigDecimal.ZERO);

        String contributionStatus;
        if (cyclesMissed <= 0) {
            contributionStatus = "UP_TO_DATE";
        } else if (cyclesMissed <= 2) {
            contributionStatus = "BEHIND";
        } else {
            contributionStatus = "DEFAULTED";
        }

        return MemberContributionDto.builder()
                .userId(user.getId())
                .userFullName(user.getFirstName() + " " + user.getLastName())
                .userEmail(user.getEmail())
                .userPhoneNumber(user.getPhoneNumber())
                .totalContributed(totalContributed)
                .cyclesPaid(cyclesPaid)
                .cyclesMissed(Math.max(0, cyclesMissed))
                .contributionStatus(contributionStatus)
                .lastPaymentDate(contributionRepository.findLastPaymentDateByUserIdAndGroupId(userId, groupId)
                        .orElse(null))
                .build();
    }

    // ============ HELPER METHODS ============

    private GroupResponse mapToResponse(Group group, int currentMembers) {
        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .contributionAmount(group.getContributionAmount())
                .cycleType(group.getCycleType())
                .status(group.getStatus())
                .maxMembers(group.getMaxMembers())
                .currentMembers(currentMembers)
                .createdByName(group.getCreatedBy().getFirstName()
                        + " " + group.getCreatedBy().getLastName())
                .createdAt(group.getCreatedAt())
                .build();
    }

    private int calculateCurrentCycle(Group group) {
        // Calculate based on group creation date and cycle type
        // This is a simplified version - you can enhance this logic
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

    @Transactional
    public void sendPaymentReminders(Long groupId, String userEmail) {
        User creator = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Verify user is the group creator
        if (!group.getCreatedBy().getId().equals(creator.getId())) {
            throw new RuntimeException("Only group creator can send reminders");
        }

        // Get all members of the group
        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);

        int currentCycle = calculateCurrentCycle(group);
        int remindersSent = 0;

        for (GroupMember member : members) {
            User memberUser = member.getUser();

            // Check if member has paid for current cycle
            boolean hasPaid = contributionRepository.existsByGroupIdAndUserIdAndCycleNumberAndStatus(
                    groupId, memberUser.getId(), currentCycle, "PAID");

            if (!hasPaid) {
                // Send email reminder
                sendReminderEmail(memberUser, group, currentCycle);
                remindersSent++;
            }
        }

        if (remindersSent == 0) {
            throw new RuntimeException("All members have already paid for this cycle");
        }
    }

    private void sendReminderEmail(User member, Group group, int cycleNumber) {
        // Use your existing notification service
        notificationService.sendEmailNotification(
                member.getEmail(),
                "Payment Reminder - " + group.getName(),
                String.format("Dear %s,\n\nThis is a reminder that your contribution of ₦%.2f for cycle %d in group '%s' is due.\n\nPlease make your payment to avoid penalties.\n\nThank you!",
                        member.getFirstName(),
                        group.getContributionAmount(),
                        cycleNumber,
                        group.getName())
        );
    }
}