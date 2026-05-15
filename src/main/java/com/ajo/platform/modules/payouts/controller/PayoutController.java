package com.ajo.platform.modules.payouts.controller;

import com.ajo.platform.modules.payouts.dto.PayoutResponse;
import com.ajo.platform.modules.payouts.dto.PayoutSummary;
import com.ajo.platform.modules.payouts.service.PayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payouts")
@RequiredArgsConstructor
public class PayoutController {

    private final PayoutService payoutService;

    @PostMapping("/group/{groupId}/cycle/{cycleNumber}/trigger")
    public ResponseEntity<PayoutResponse> triggerPayout(
            @PathVariable Long groupId,
            @PathVariable Integer cycleNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                payoutService.triggerPayout(groupId, cycleNumber, userDetails.getUsername()));
    }

    @GetMapping("/group/{groupId}/cycle/{cycleNumber}/summary")
    public ResponseEntity<PayoutSummary> getCycleSummary(
            @PathVariable Long groupId,
            @PathVariable Integer cycleNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                payoutService.getCycleSummary(groupId, cycleNumber, userDetails.getUsername()));
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<PayoutResponse>> getGroupPayouts(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                payoutService.getGroupPayouts(groupId, userDetails.getUsername()));
    }

    @GetMapping("/my-payouts")
    public ResponseEntity<List<PayoutResponse>> getMyPayouts(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                payoutService.getMyPayouts(userDetails.getUsername()));
    }
}