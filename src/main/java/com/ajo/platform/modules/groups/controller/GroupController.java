package com.ajo.platform.modules.groups.controller;

import com.ajo.platform.modules.groups.dto.CreateGroupRequest;
import com.ajo.platform.modules.groups.dto.GroupResponse;
import com.ajo.platform.modules.groups.dto.JoinGroupRequest;
import com.ajo.platform.modules.groups.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

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
}