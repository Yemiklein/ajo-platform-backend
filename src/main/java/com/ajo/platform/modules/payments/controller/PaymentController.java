package com.ajo.platform.modules.payments.controller;

import com.ajo.platform.modules.payments.dto.InitializePaymentRequest;
import com.ajo.platform.modules.payments.dto.InitializePaymentResponse;
import com.ajo.platform.modules.payments.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initialize")
    public ResponseEntity<InitializePaymentResponse> initializePayment(
            @Valid @RequestBody InitializePaymentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) throws Exception {
        return ResponseEntity.ok(
                paymentService.initializePayment(request, userDetails.getUsername()));
    }

    @PostMapping("/webhook/paystack")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("x-paystack-signature") String signature) throws Exception {
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}