package com.ajo.platform.modules.admin.controller;

import com.ajo.platform.modules.admin.service.AdminService;
import com.ajo.platform.modules.fraud.model.FraudAlert;
import com.ajo.platform.modules.groups.dto.GroupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // ===== Fraud Alerts =====
    @GetMapping("/fraud-alerts")
    public ResponseEntity<List<FraudAlert>> getAllFraudAlerts() {
        return ResponseEntity.ok(adminService.getAllFraudAlerts());
    }

    @PatchMapping("/fraud-alerts/{id}")
    public ResponseEntity<FraudAlert> updateFraudAlertStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return ResponseEntity.ok(adminService.updateFraudAlertStatus(id, status));
    }

    // ===== Groups =====
    @GetMapping("/groups")
    public ResponseEntity<List<GroupResponse>> getAllGroups() {
        return ResponseEntity.ok(adminService.getAllGroups());
    }

    @PatchMapping("/groups/{id}/status")
    public ResponseEntity<GroupResponse> updateGroupStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return ResponseEntity.ok(adminService.updateGroupStatus(id, status));
    }
}