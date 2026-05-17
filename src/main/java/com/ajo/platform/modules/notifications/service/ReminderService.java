package com.ajo.platform.modules.notifications.service;

import com.ajo.platform.modules.contributions.repository.ContributionRepository;
import com.ajo.platform.modules.groups.model.Group;
import com.ajo.platform.modules.groups.repository.GroupMemberRepository;
import com.ajo.platform.modules.groups.repository.GroupRepository;
import com.ajo.platform.modules.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@EnableScheduling
public class ReminderService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ContributionRepository contributionRepository;
    private final NotificationService notificationService;

    // Run every day at 9 AM
    @Scheduled(cron = "0 0 9 * * *")
    public void sendContributionReminders() {
        List<Group> activeGroups = groupRepository.findByStatus(Group.GroupStatus.ACTIVE);

        for (Group group : activeGroups) {
            int currentCycle = calculateCurrentCycle(group);
            var members = groupMemberRepository.findByGroupIdAndStatus(group.getId(), "ACTIVE");

            for (var member : members) {
                boolean hasPaid = contributionRepository.existsByGroupIdAndUserIdAndCycleNumber(
                        group.getId(), member.getUser().getId(), currentCycle);

                if (!hasPaid) {
                    String dueDate = calculateDueDate(group, currentCycle).toString();
                    notificationService.sendEmailNotification(
                            member.getUser().getEmail(),
                            "Contribution Reminder - " + group.getName(),
                            String.format(
                                    "Dear %s,\n\nThis is a reminder that your contribution of ₦%.2f for cycle %d of group '%s' is due by %s.\n\nPlease make your payment to avoid penalties.\n\nThank you!",
                                    member.getUser().getFirstName(),
                                    group.getContributionAmount(),
                                    currentCycle,
                                    group.getName(),
                                    dueDate
                            )
                    );
                }
            }
        }
    }

    private int calculateCurrentCycle(Group group) {
        // Calculate based on group creation date and cycle type
        // This is a simplified version
        return 1;
    }

    private LocalDateTime calculateDueDate(Group group, int cycleNumber) {
        // Calculate based on group creation date and cycle type
        return LocalDateTime.now().plusDays(3);
    }
}