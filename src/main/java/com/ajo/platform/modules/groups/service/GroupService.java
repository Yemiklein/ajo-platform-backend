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
import com.ajo.platform.modules.contributions.model.Contribution;

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

        if (!group.getCreatedBy().getId().equals(invitedBy.getId())) {
            throw new RuntimeException("Only group creator can invite members");
        }

        if (group.getStatus() == Group.GroupStatus.COMPLETED || group.getStatus() == Group.GroupStatus.CANCELLED) {
            throw new RuntimeException("Group is no longer active");
        }

        int currentMembers = groupMemberRepository.countByGroupId(groupId);
        if (currentMembers >= group.getMaxMembers()) {
            throw new RuntimeException("Group has reached maximum member limit");
        }

        User invitedUser = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (invitedUser != null && groupMemberRepository.existsByGroupIdAndUserId(groupId, invitedUser.getId())) {
            throw new RuntimeException("User is already a member of this group");
        }

        groupInviteRepository.deleteByGroupIdAndInvitedEmail(groupId, request.getEmail());

        String inviteCode = UUID.randomUUID().toString();

        // Generate the correct invite link based on environment
        String inviteLink = generateInviteLink(inviteCode);

        GroupInvite invite = GroupInvite.builder()
                .inviteCode(inviteCode)
                .group(group)
                .invitedBy(invitedBy)
                .invitedEmail(request.getEmail())
                .status(GroupInvite.InviteStatus.PENDING)
                .build();

        groupInviteRepository.save(invite);

        try {
            notificationService.sendEmailNotification(
                    request.getEmail(),
                    "Invitation to join " + group.getName() + " on Ajo Platform",
                    String.format("Hello,\n\n%s has invited you to join their savings group '%s'.\n\n" +
                                    "Click the link below to join:\n%s\n\n" +
                                    "Message from %s: %s\n\n" +
                                    "This invitation expires in 7 days.\n\n" +
                                    "Best regards,\nAjo Platform Team",
                            invitedBy.getFirstName() + " " + invitedBy.getLastName(),
                            group.getName(),
                            inviteLink,
                            invitedBy.getFirstName(),
                            request.getMessage() != null ? request.getMessage() : "No additional message"
                    )
            );
        } catch (Exception e) {
            System.out.println("Failed to send email: " + e.getMessage());
        }

        return InviteResponse.builder()
                .inviteId(invite.getId())
                .inviteCode(inviteCode)
                .inviteLink(inviteLink)
                .message(request.getMessage())
                .expiresAt(invite.getExpiresAt().toString())
                .build();
    }

    // Helper method to generate invite link based on environment
    private String generateInviteLink(String inviteCode) {
        // Try to get from environment variable first
        String frontendUrl = System.getenv("FRONTEND_URL");

        if (frontendUrl != null && !frontendUrl.isEmpty()) {
            return frontendUrl + "/join/" + inviteCode;
        }

        // Fallback to checking Spring profile
        String profile = System.getProperty("spring.profiles.active");

        if (profile != null && profile.equals("production")) {
            return "https://ajo-platform-frontend.vercel.app/join/" + inviteCode;
        } else if (profile != null && profile.equals("staging")) {
            return "https://staging.ajo-platform.vercel.app/join/" + inviteCode;
        } else {
            // Default to localhost for development
            return "http://localhost:3000/join/" + inviteCode;
        }
    }

    @Transactional
    public GroupResponse joinViaInvite(String inviteCode, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        GroupInvite invite = groupInviteRepository.findByInviteCodeAndStatus(inviteCode, GroupInvite.InviteStatus.PENDING)
                .orElseThrow(() -> new RuntimeException("Invalid or expired invite link"));

        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            invite.setStatus(GroupInvite.InviteStatus.EXPIRED);
            groupInviteRepository.save(invite);
            throw new RuntimeException("Invite link has expired");
        }

        Group group = invite.getGroup();

        if (group.getStatus() == Group.GroupStatus.COMPLETED || group.getStatus() == Group.GroupStatus.CANCELLED) {
            throw new RuntimeException("Group is no longer active");
        }

        int currentMembers = groupMemberRepository.countByGroupId(group.getId());
        if (currentMembers >= group.getMaxMembers()) {
            invite.setStatus(GroupInvite.InviteStatus.EXPIRED);
            groupInviteRepository.save(invite);
            throw new RuntimeException("Group has reached maximum member limit");
        }

        if (groupMemberRepository.existsByGroupIdAndUserId(group.getId(), user.getId())) {
            throw new RuntimeException("You are already a member of this group");
        }

        GroupMember member = GroupMember.builder()
                .group(group)
                .user(user)
                .payoutPosition(currentMembers + 1)
                .status(GroupMember.MemberStatus.ACTIVE)
                .build();

        groupMemberRepository.save(member);

        invite.setStatus(GroupInvite.InviteStatus.ACCEPTED);
        invite.setAcceptedAt(LocalDateTime.now());
        groupInviteRepository.save(invite);

        if (currentMembers + 1 >= group.getMaxMembers()) {
            group.setStatus(Group.GroupStatus.ACTIVE);
            groupRepository.save(group);
        }

        return mapToResponse(group, currentMembers + 1);
    }

    // ============ NEW FEATURE 2: MEMBER CONTRIBUTION TRACKING ============

    public GroupContributionSummaryDto getContributionSummary(Long groupId, String userEmail) {
        System.out.println("=== DEBUG: getContributionSummary ===");
        System.out.println("Group ID: " + groupId);
        System.out.println("User Email: " + userEmail);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        System.out.println("Group found: " + group.getName());
        System.out.println("Group status: " + group.getStatus());
        System.out.println("Group creator ID: " + group.getCreatedBy().getId());

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        System.out.println("User ID: " + user.getId());
        System.out.println("User role: " + user.getRole().name());

        boolean isAdmin = "ADMIN".equals(user.getRole().name());
        boolean isCreator = group.getCreatedBy().getEmail().equalsIgnoreCase(user.getEmail());
        boolean isMember = groupMemberRepository.existsByGroupIdAndUserId(groupId, user.getId());

        System.out.println("Is Admin: " + isAdmin);
        System.out.println("Is Creator: " + isCreator);
        System.out.println("Is Member: " + isMember);

        if (!isAdmin && !isCreator && !isMember) {
            System.out.println("Access denied!");
            throw new RuntimeException("You are not authorized to view this group's contributions");
        }


        System.out.println("Access granted!");

        List<GroupMember> allMembers = groupMemberRepository.findByGroupId(groupId);
        List<GroupMember> activeMembers = allMembers.stream()
                .filter(m -> GroupMember.MemberStatus.ACTIVE.equals(m.getStatus()))
                .collect(Collectors.toList());

        BigDecimal totalExpectedAmount = group.getContributionAmount()
                .multiply(BigDecimal.valueOf(activeMembers.size()));

        // FIXED: Removed "PAID" parameter
        BigDecimal totalReceivedAmount = contributionRepository.sumAmountByGroupIdAndStatus(groupId)
                .orElse(BigDecimal.ZERO);

        BigDecimal collectionRate = totalExpectedAmount.compareTo(BigDecimal.ZERO) > 0
                ? totalReceivedAmount.multiply(BigDecimal.valueOf(100))
                .divide(totalExpectedAmount, 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<MemberContributionDto> memberContributions = activeMembers.stream()
                .map(member -> getMemberContributionSummary(groupId, member.getUser().getId()))
                .collect(Collectors.toList());

        return GroupContributionSummaryDto.builder()
                .groupId(groupId)
                .groupName(group.getName())
                .totalMembers(activeMembers.size())
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

        User requestingUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = "ADMIN".equals(requestingUser.getRole().name());
        boolean isMember = groupMemberRepository.existsByGroupIdAndUserId(groupId, requestingUser.getId());

        if (!isAdmin && !isMember) {
            throw new RuntimeException("You are not authorized to view this information");
        }

        return getMemberContributionSummary(groupId, memberId);
    }

    private MemberContributionDto getMemberContributionSummary(Long groupId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // FIXED: Removed "PAID" parameter
        int cyclesPaid = contributionRepository.countByGroupIdAndUserIdAndStatus(groupId, userId);
        int currentCycle = calculateCurrentCycle(group);
        int cyclesMissed = Math.max(0, currentCycle - cyclesPaid);

        // FIXED: Removed "PAID" parameter
        BigDecimal totalContributed = contributionRepository.sumAmountByGroupIdAndUserIdAndStatus(groupId, userId)
                .orElse(BigDecimal.ZERO);

        String contributionStatus;
        if (cyclesMissed == 0) {
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
                .cyclesMissed(cyclesMissed)
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
                .createdBy(GroupResponse.CreatedByInfo.builder()
                        .id(group.getCreatedBy().getId())
                        .firstName(group.getCreatedBy().getFirstName())
                        .lastName(group.getCreatedBy().getLastName())
                        .build())
                .createdByName(group.getCreatedBy().getFirstName()
                        + " " + group.getCreatedBy().getLastName())
                .createdAt(group.getCreatedAt())
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

    @Transactional
    public void sendPaymentReminders(Long groupId, String userEmail) {
        System.out.println("=== sendPaymentReminders START ===");

        // Get the user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
        System.out.println("User: " + user.getEmail() + ", Role: " + user.getRole());

        // Get the group
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));
        System.out.println("Group: " + group.getName() + ", Creator: " + group.getCreatedBy().getEmail());

        // Permission check - allow ADMIN or CREATOR
        boolean isAdmin = user.getRole().name().equals("ADMIN");
        boolean isCreator = group.getCreatedBy().getId().equals(user.getId());

        System.out.println("isAdmin: " + isAdmin);
        System.out.println("isCreator: " + isCreator);

        if (!isAdmin && !isCreator) {
            throw new RuntimeException("Only admin or group creator can send reminders");
        }

        // Get all members
        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        System.out.println("Total members: " + members.size());

        int currentCycle = calculateCurrentCycle(group);
        int remindersSent = 0;

        for (GroupMember member : members) {
            User memberUser = member.getUser();
            System.out.println("Checking member: " + memberUser.getEmail());

            // Simple check - if no contributions exist, send reminder
            boolean hasPaid = contributionRepository.existsByGroupIdAndUserIdAndCycleNumber(
                    groupId, memberUser.getId(), currentCycle);

            System.out.println("Has paid: " + hasPaid);

            if (!hasPaid) {
                sendReminderEmail(memberUser, group, currentCycle);
                remindersSent++;
            }
        }

        System.out.println("Reminders sent: " + remindersSent);

    }

    private void sendReminderEmail(User member, Group group, int cycleNumber) {
        try {
            // Send Email
            notificationService.sendEmailNotification(
                    member.getEmail(),
                    "Payment Reminder - " + group.getName(),
                    String.format("Dear %s,\n\nThis is a reminder that your contribution of ₦%.2f for cycle %d in group '%s' is due.\n\nThank you!",
                            member.getFirstName(),
                            group.getContributionAmount(),
                            cycleNumber,
                            group.getName())
            );

            // Send SMS (if you want)
            if (member.getPhoneNumber() != null && !member.getPhoneNumber().isEmpty()) {
                notificationService.sendSmsNotification(
                        member.getPhoneNumber(),
                        String.format("Ajo Reminder: Your payment of ₦%.0f for group '%s' cycle %d is due.",
                                group.getContributionAmount(),
                                group.getName(),
                                cycleNumber)
                );
            }

        } catch (Exception e) {
            System.out.println("Failed to send reminder to " + member.getEmail() + ": " + e.getMessage());
        }
    }
}