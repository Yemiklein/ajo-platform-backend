package com.ajo.platform.modules.contributions.controller;

import com.ajo.platform.modules.contributions.dto.ContributeRequest;
import com.ajo.platform.modules.contributions.dto.ContributionResponse;
import com.ajo.platform.modules.contributions.service.ContributionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contributions")
@RequiredArgsConstructor
public class ContributionController {

    private final ContributionService contributionService;

    @PostMapping
    public ResponseEntity<ContributionResponse> contribute(
            @Valid @RequestBody ContributeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                contributionService.contribute(request, userDetails.getUsername()));
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<ContributionResponse>> getMyContributions(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                contributionService.getGroupContributions(groupId, userDetails.getUsername()));
    }

    @GetMapping("/group/{groupId}/cycle/{cycleNumber}")
    public ResponseEntity<List<ContributionResponse>> getCycleContributions(
            @PathVariable Long groupId,
            @PathVariable Integer cycleNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                contributionService.getCycleContributions(groupId, cycleNumber,
                        userDetails.getUsername()));
    }

    @GetMapping("/group/{groupId}/cycle/{cycleNumber}/progress")
    public ResponseEntity<Integer> getCycleProgress(
            @PathVariable Long groupId,
            @PathVariable Integer cycleNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                contributionService.getCycleProgress(groupId, cycleNumber,
                        userDetails.getUsername()));
    }
}