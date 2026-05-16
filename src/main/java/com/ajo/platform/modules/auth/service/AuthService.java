package com.ajo.platform.modules.auth.service;

import com.ajo.platform.modules.auth.dto.AuthResponse;
import com.ajo.platform.modules.auth.dto.LoginRequest;
import com.ajo.platform.modules.auth.dto.RegisterRequest;
import com.ajo.platform.modules.auth.model.User;
import com.ajo.platform.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.ajo.platform.modules.auth.dto.ForgotPasswordRequest;
import com.ajo.platform.modules.auth.dto.ResetPasswordRequest;
import com.ajo.platform.modules.auth.dto.VerifyOtpRequest;
import com.ajo.platform.modules.auth.model.PasswordResetToken;
import com.ajo.platform.modules.auth.repository.PasswordResetTokenRepository;
import com.ajo.platform.modules.notifications.service.NotificationService;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${admin.registration.secret}")
    private String adminRegistrationSecret;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final NotificationService notificationService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("Phone number already registered");
        }

        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(User.Role.USER)
                .enabled(true)
                .build();

        userRepository.save(user);

        var token = jwtService.generateToken(
                new org.springframework.security.core.userdetails.User(
                        user.getEmail(),
                        user.getPassword(),
                        java.util.Collections.emptyList()
                )
        );

        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getRole().name(),
                user.isEnabled()
        );
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        var token = jwtService.generateToken(
                new org.springframework.security.core.userdetails.User(
                        user.getEmail(),
                        user.getPassword(),
                        java.util.Collections.emptyList()
                )
        );

        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getRole().name(),
                user.isEnabled()
        );
    }

    public AuthResponse registerAdmin(RegisterRequest request, String adminSecret) {
        if (adminRegistrationSecret == null || !adminRegistrationSecret.equals(adminSecret)) {
            throw new RuntimeException("Invalid admin secret");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("Phone number already registered");
        }

        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(User.Role.ADMIN)
                .enabled(true)
                .build();

        userRepository.save(user);

        var token = jwtService.generateToken(
                new org.springframework.security.core.userdetails.User(
                        user.getEmail(),
                        user.getPassword(),
                        java.util.Collections.emptyList()
                )
        );

        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getRole().name(),
                user.isEnabled()
        );
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        String identifier = request.getIdentifier().trim();
        String type = request.getType().toUpperCase();

        // Find user by email or phone
        User user;
        if (type.equals("EMAIL")) {
            user = userRepository.findByEmail(identifier)
                    .orElseThrow(() -> new RuntimeException("No account found with this email"));
        } else {
            user = userRepository.findByPhoneNumber(identifier)
                    .orElseThrow(() -> new RuntimeException("No account found with this phone number"));
        }

        // Delete any existing tokens for this identifier
        passwordResetTokenRepository.deleteByIdentifier(identifier);

        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Save token
        PasswordResetToken token = PasswordResetToken.builder()
                .otp(otp)
                .identifier(identifier)
                .type(type)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        passwordResetTokenRepository.save(token);

        // Send OTP
        if (type.equals("EMAIL")) {
            notificationService.sendEmailNotification(
                    identifier,
                    "Password Reset OTP - Ajo Platform",
                    "Your password reset OTP is: " + otp + "\n\nThis code expires in 10 minutes.\n\nIf you did not request this, please ignore this email."
            );
        } else {
            notificationService.sendSmsNotification(
                    identifier,
                    "Your Ajo Platform password reset OTP is: " + otp + ". Valid for 10 minutes."
            );
        }
    }

    public void verifyOtp(VerifyOtpRequest request) {
        PasswordResetToken token = passwordResetTokenRepository
                .findByOtpAndIdentifierAndUsedFalse(request.getOtp(), request.getIdentifier())
                .orElseThrow(() -> new RuntimeException("Invalid or expired OTP"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired. Please request a new one");
        }
    }

    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository
                .findByOtpAndIdentifierAndUsedFalse(request.getOtp(), request.getIdentifier())
                .orElseThrow(() -> new RuntimeException("Invalid or expired OTP"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired. Please request a new one");
        }

        // Update password
        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark token as used
        token.setUsed(true);
        passwordResetTokenRepository.save(token);
    }

}