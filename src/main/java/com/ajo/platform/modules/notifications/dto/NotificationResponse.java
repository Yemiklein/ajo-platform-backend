package com.ajo.platform.modules.notifications.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationResponse {

    private boolean smsSent;
    private boolean emailSent;
    private String message;
}