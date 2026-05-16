package com.ajo.platform.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank
    private String identifier;

    @NotBlank
    private String otp;
}