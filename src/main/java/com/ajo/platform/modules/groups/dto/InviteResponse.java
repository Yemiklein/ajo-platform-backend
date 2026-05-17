package com.ajo.platform.modules.groups.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InviteResponse {
    private Long inviteId;
    private String inviteCode;
    private String inviteLink;
    private String message;
    private String expiresAt;
}