package com.ajo.platform.modules.admin.service;

import com.ajo.platform.modules.fraud.model.FraudAlert;
import com.ajo.platform.modules.fraud.repository.FraudAlertRepository;
import com.ajo.platform.modules.groups.dto.GroupResponse;
import com.ajo.platform.modules.groups.model.Group;
import com.ajo.platform.modules.groups.repository.GroupMemberRepository;
import com.ajo.platform.modules.groups.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final FraudAlertRepository fraudAlertRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    // ===== Fraud Alerts =====
    public List<FraudAlert> getAllFraudAlerts() {
        return fraudAlertRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public FraudAlert updateFraudAlertStatus(Long id, String status) {
        FraudAlert alert = fraudAlertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fraud alert not found"));

        alert.setStatus(FraudAlert.AlertStatus.valueOf(status));

        if (status.equals("RESOLVED") || status.equals("FALSE_POSITIVE")) {
            alert.setResolvedAt(LocalDateTime.now());
        }

        return fraudAlertRepository.save(alert);
    }

    // ===== Groups =====
    public List<GroupResponse> getAllGroups() {
        return groupRepository.findAll()
                .stream()
                .map(group -> mapToResponse(group,
                        groupMemberRepository.countByGroupId(group.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public GroupResponse updateGroupStatus(Long id, String status) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        group.setStatus(Group.GroupStatus.valueOf(status));
        groupRepository.save(group);

        int memberCount = groupMemberRepository.countByGroupId(id);
        return mapToResponse(group, memberCount);
    }

    private GroupResponse mapToResponse(Group group, int currentMembers) {
        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .contributionAmount(group.getContributionAmount())
                .cycleType(group.getCycleType())
                .status(group.getStatus())
                .maxMembers(group.getMaxMembers())
                .currentMembers(currentMembers)
                .createdByName(group.getCreatedBy().getFirstName()
                        + " " + group.getCreatedBy().getLastName())
                .createdAt(group.getCreatedAt())
                .build();
    }
}