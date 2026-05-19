package com.ajo.platform.modules.users.service;

import com.ajo.platform.modules.auth.model.User;
import com.ajo.platform.modules.auth.repository.UserRepository;
import com.ajo.platform.modules.users.dto.BankAccountRequest;
import com.ajo.platform.modules.users.dto.BankAccountResponse;
import com.ajo.platform.modules.users.dto.UpdateProfileRequest;
import com.ajo.platform.modules.users.dto.UserProfileResponse;
import com.ajo.platform.modules.users.model.BankAccount;
import com.ajo.platform.modules.users.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;

    public BankAccountResponse getBankAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return bankAccountRepository.findByUserId(user.getId())
                .map(this::mapToResponse)
                .orElse(null);
    }

    @Transactional
    public BankAccountResponse saveBankAccount(BankAccountRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        BankAccount bankAccount = bankAccountRepository.findByUserId(user.getId())
                .orElse(BankAccount.builder().user(user).build());

        bankAccount.setAccountNumber(request.getAccountNumber());
        bankAccount.setBankCode(request.getBankCode());
        bankAccount.setBankName(request.getBankName());
        bankAccount.setAccountName(request.getAccountName());

        bankAccountRepository.save(bankAccount);
        return mapToResponse(bankAccount);
    }

    @Transactional
    public void deleteBankAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        bankAccountRepository.findByUserId(user.getId())
                .ifPresent(bankAccountRepository::delete);
    }

    @Transactional
    public UserProfileResponse updateProfile(UpdateProfileRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            if (!request.getPhoneNumber().equals(user.getPhoneNumber())
                    && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new RuntimeException("Phone number already in use");
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }

        userRepository.save(user);

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().name())
                .build();
    }

    private BankAccountResponse mapToResponse(BankAccount bankAccount) {
        return BankAccountResponse.builder()
                .id(bankAccount.getId())
                .accountNumber(bankAccount.getAccountNumber())
                .bankCode(bankAccount.getBankCode())
                .bankName(bankAccount.getBankName())
                .accountName(bankAccount.getAccountName())
                .createdAt(bankAccount.getCreatedAt())
                .updatedAt(bankAccount.getUpdatedAt())
                .build();
    }
}