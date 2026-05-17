package com.ajo.platform.modules.groups.controller;

import com.ajo.platform.modules.contributions.dto.ContributionSummaryResponse;
import com.ajo.platform.modules.contributions.service.ContributionService;
import com.ajo.platform.modules.groups.dto.*;
import com.ajo.platform.modules.groups.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final ContributionService contributionService;  // ADD THIS

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(groupService.createGroup(request, userDetails.getUsername()));
    }

    @PostMapping("/join")
    public ResponseEntity<GroupResponse> joinGroup(
            @Valid @RequestBody JoinGroupRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(groupService.joinGroup(request.getGroupId(), userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getMyGroups(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(groupService.getMyGroups(userDetails.getUsername()));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponse> getGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(groupService.getGroup(groupId, userDetails.getUsername()));
    }

    // Invite System Endpoints
    @PostMapping("/{groupId}/invite")
    public ResponseEntity<InviteResponse> inviteMember(
            @PathVariable Long groupId,
            @Valid @RequestBody InviteMemberRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(groupService.inviteMember(groupId, request, userDetails.getUsername()));
    }

    @PostMapping("/join/{inviteCode}")
    public ResponseEntity<GroupResponse> joinViaInvite(
            @PathVariable String inviteCode,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(groupService.joinViaInvite(inviteCode, userDetails.getUsername()));
    }

    // ============ CONTRIBUTION SUMMARY ENDPOINT (SINGLE VERSION) ============
    @GetMapping("/{groupId}/contributions/summary")
    public ResponseEntity<ContributionSummaryResponse> getContributionSummary(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                contributionService.getGroupContributionSummary(groupId, userDetails.getUsername()));
    }

    @GetMapping("/{groupId}/members/{memberId}/contributions")
    public ResponseEntity<MemberContributionDto> getMemberContributions(
            @PathVariable Long groupId,
            @PathVariable Long memberId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(groupService.getMemberContributions(groupId, memberId, userDetails.getUsername()));
    }

    @PostMapping("/{groupId}/send-reminders")
    public ResponseEntity<Map<String, String>> sendReminders(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserDetails userDetails) {

        System.out.println("=== GroupController.sendReminders ===");
        System.out.println("userDetails: " + (userDetails != null ? userDetails.getUsername() : "NULL"));

        if (userDetails == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(error);
        }

        groupService.sendPaymentReminders(groupId, userDetails.getUsername());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Reminders sent successfully to members who haven't paid");
        return ResponseEntity.ok(response);
    }
}