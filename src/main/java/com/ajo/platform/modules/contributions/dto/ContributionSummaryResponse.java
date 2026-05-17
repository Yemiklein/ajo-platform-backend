package com.ajo.platform.modules.contributions.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class ContributionSummaryResponse {
    private long totalContributions;
    private BigDecimal totalAmount;
    private int totalMembers;
    private double collectionRate;
    private int expectedContributions;
    private String groupName;
    private int currentCycle;
}