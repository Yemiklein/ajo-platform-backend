package com.ajo.platform.modules.users.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BankAccountRequest {

    @NotBlank
    private String accountNumber;

    @NotBlank
    private String bankCode;

    @NotBlank
    private String bankName;

    @NotBlank
    private String accountName;
}