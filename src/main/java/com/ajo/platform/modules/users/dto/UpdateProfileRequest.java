package com.ajo.platform.modules.users.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String phoneNumber;
}