package com.ajo.platform.modules.groups.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class GroupContributionSummaryDto {
    private Long groupId;
    private String groupName;
    private int totalMembers;
    private int currentCycle;
    private BigDecimal totalExpectedAmount;
    private BigDecimal totalReceivedAmount;
    private BigDecimal collectionRate; // percentage
    private List<MemberContributionDto> memberContributions;
}