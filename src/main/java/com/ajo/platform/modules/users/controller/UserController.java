package com.ajo.platform.modules.users.controller;

import com.ajo.platform.modules.users.dto.BankAccountRequest;
import com.ajo.platform.modules.users.dto.BankAccountResponse;
import com.ajo.platform.modules.users.dto.UpdateProfileRequest;
import com.ajo.platform.modules.users.dto.UserProfileResponse;
import com.ajo.platform.modules.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.updateProfile(request, userDetails.getUsername()));
    }

    @GetMapping("/me/bank-account")
    public ResponseEntity<BankAccountResponse> getBankAccount(
            @AuthenticationPrincipal UserDetails userDetails) {
        BankAccountResponse response = userService.getBankAccount(userDetails.getUsername());
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/bank-account")
    public ResponseEntity<BankAccountResponse> saveBankAccount(
            @Valid @RequestBody BankAccountRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.saveBankAccount(request, userDetails.getUsername()));
    }

    @DeleteMapping("/me/bank-account")
    public ResponseEntity<Void> deleteBankAccount(
            @AuthenticationPrincipal UserDetails userDetails) {
        userService.deleteBankAccount(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}