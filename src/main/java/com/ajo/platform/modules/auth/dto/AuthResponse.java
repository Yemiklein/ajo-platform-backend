package com.ajo.platform.modules.auth.dto;

import com.ajo.platform.modules.auth.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String role;
    private boolean enabled;
}