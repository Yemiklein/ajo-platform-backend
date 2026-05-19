package com.ajo.platform.modules.users.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String role;
}