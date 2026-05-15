package com.ajo.platform.modules.contributions.dto;

import com.ajo.platform.modules.contributions.model.Contribution;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ContributionResponse {

    private Long id;
    private Long groupId;
    private String groupName;
    private String memberName;
    private Integer cycleNumber;
    private BigDecimal amount;
    private Contribution.ContributionStatus status;
    private String idempotencyKey;
    private String paymentReference;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}