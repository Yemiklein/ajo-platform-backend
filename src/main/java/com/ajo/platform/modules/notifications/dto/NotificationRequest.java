package com.ajo.platform.modules.notifications.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationRequest {

    private String recipient;
    private String subject;
    private String message;
    private NotificationType type;

    public enum NotificationType {
        SMS, EMAIL, BOTH
    }
}