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
    private String createdByName;
    private LocalDateTime createdAt;
}