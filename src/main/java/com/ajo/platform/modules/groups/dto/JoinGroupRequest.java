package com.ajo.platform.modules.groups.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JoinGroupRequest {

    @NotNull(message = "Group ID is required")
    private Long groupId;
}