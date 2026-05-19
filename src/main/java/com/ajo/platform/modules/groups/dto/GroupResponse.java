package com.ajo.platform.modules.groups.dto;

import com.ajo.platform.modules.groups.model.Group;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class GroupResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal contributionAmount;
    private Group.CycleType cycleType;
    private Group.GroupStatus status;
    private Integer maxMembers;
    private Integer currentMembers;
    private CreatedByInfo createdBy;
    private String createdByName;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class CreatedByInfo {
        private Long id;
        private String firstName;
        private String lastName;
    }
}