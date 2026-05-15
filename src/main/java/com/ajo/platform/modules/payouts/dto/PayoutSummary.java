package com.ajo.platform.modules.payouts.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PayoutSummary {

    private Integer cycleNumber;
    private String recipientName;
    private BigDecimal amount;
    private String status;
    private boolean allMembersPaid;
    private int paidMembersCount;
    private int totalMembers;
}