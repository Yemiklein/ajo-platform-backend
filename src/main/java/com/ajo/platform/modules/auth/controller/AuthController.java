package com.ajo.platform.modules.auth.controller;

import com.ajo.platform.modules.auth.dto.AuthResponse;
import com.ajo.platform.modules.auth.dto.LoginRequest;
import com.ajo.platform.modules.auth.dto.RegisterRequest;
import com.ajo.platform.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request)  {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register-admin")
    public ResponseEntity<AuthResponse> registerAdmin(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader("X-Admin-Secret") String adminSecret) {
        return ResponseEntity.ok(authService.registerAdmin(request, adminSecret));
    }
}