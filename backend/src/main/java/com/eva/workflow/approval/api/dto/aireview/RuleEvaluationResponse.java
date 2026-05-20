package com.eva.workflow.approval.api.dto.aireview;

import java.time.Instant;
import java.util.List;

import com.eva.workflow.approval.common.enums.RiskLevel;

public record RuleEvaluationResponse(
        Long requestId,
        Instant evaluatedAt,
        RiskLevel highestRiskLevel,
        List<HardRuleFlagResponse> flags
) {
}
