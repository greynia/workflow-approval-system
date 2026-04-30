package com.eva.workflow.approval.api.dto.aireview;

import com.eva.workflow.approval.common.enums.RiskLevel;

public record HardRuleFlagResponse(
        String code,
        RiskLevel level,
        String humanReadable
) {
}
