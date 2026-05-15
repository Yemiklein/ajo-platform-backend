package com.ajo.platform.modules.fraud.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FraudCheckRequest {

    private Long userId;
    private Long groupId;
    private Integer cycleNumber;
    private String action;
}