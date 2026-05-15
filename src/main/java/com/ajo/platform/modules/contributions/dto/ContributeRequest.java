package com.ajo.platform.modules.contributions.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ContributeRequest {

    @NotNull(message = "Group ID is required")
    private Long groupId;

    @NotNull(message = "Cycle number is required")
    private Integer cycleNumber;

    private String paymentReference;
}