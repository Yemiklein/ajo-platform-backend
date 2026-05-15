package com.ajo.platform.modules.groups.dto;

import com.ajo.platform.modules.groups.model.Group;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateGroupRequest {

    @NotBlank(message = "Group name is required")
    private String name;

    private String description;

    @NotNull(message = "Contribution amount is required")
    @DecimalMin(value = "100.00", message = "Minimum contribution is ₦100")
    private BigDecimal contributionAmount;

    @NotNull(message = "Cycle type is required")
    private Group.CycleType cycleType;

    @NotNull(message = "Max members is required")
    @Min(value = 2, message = "Group must have at least 2 members")
    @Max(value = 50, message = "Group cannot exceed 50 members")
    private Integer maxMembers;
}