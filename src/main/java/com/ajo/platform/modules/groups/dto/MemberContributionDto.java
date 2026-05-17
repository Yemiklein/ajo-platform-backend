package com.ajo.platform.modules.groups.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class MemberContributionDto {
    private Long userId;
    private String userFullName;
    private String userEmail;
    private String userPhoneNumber;
    private BigDecimal totalContributed;
    private int cyclesPaid;
    private int cyclesMissed;
    private String contributionStatus; // UP_TO_DATE, BEHIND, DEFAULTED
    private LocalDateTime lastPaymentDate;
}