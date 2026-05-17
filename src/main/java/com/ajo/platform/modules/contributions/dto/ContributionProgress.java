package com.ajo.platform.modules.contributions.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContributionProgress {
    private Integer cycleNumber;
    private Integer totalMembers;
    private Integer paidCount;
    private Integer pendingCount;
    private List<ContributionResponse> contributions;
}