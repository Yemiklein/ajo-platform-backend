package com.ajo.platform.modules.payouts.dto;

import com.ajo.platform.modules.payouts.model.Payout;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PayoutResponse {

    private Long id;
    private Long groupId;
    private String groupName;
    private RecipientInfo recipient;
    private String recipientName;
    private String recipientEmail;
    private Integer cycleNumber;
    private BigDecimal amount;
    private Payout.PayoutStatus status;
    private String paymentReference;
    private String narration;
    private LocalDateTime createdAt;
    private LocalDateTime disbursedAt;

    @Data
    @Builder
    public static class RecipientInfo {
        private Long id;
        private String firstName;
        private String lastName;
    }
}