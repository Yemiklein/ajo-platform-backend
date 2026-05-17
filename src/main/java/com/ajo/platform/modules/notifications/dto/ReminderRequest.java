package com.ajo.platform.modules.notifications.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReminderRequest {
    private Long groupId;
    private Integer cycleNumber;
    private String reminderType; // CONTRIBUTION, PAYOUT, OVERDUE
    private LocalDateTime scheduledFor;
}