package com.ajo.platform.modules.payments.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InitializePaymentRequest {

    @NotNull(message = "Group ID is required")
    private Long groupId;

    @NotNull(message = "Cycle number is required")
    private Integer cycleNumber;
}