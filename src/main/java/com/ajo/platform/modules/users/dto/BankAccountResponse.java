package com.ajo.platform.modules.users.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BankAccountResponse {
    private Long id;
    private String accountNumber;
    private String bankCode;
    private String bankName;
    private String accountName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}