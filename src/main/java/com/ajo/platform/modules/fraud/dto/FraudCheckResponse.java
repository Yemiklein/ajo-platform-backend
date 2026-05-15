package com.ajo.platform.modules.fraud.dto;

import com.ajo.platform.modules.fraud.model.FraudAlert;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FraudCheckResponse {

    private boolean flagged;
    private int riskScore;
    private FraudAlert.RiskLevel riskLevel;
    private List<String> reasons;
    private String recommendation;
}