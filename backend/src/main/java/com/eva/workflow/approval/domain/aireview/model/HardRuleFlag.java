package com.eva.workflow.approval.domain.aireview.model;

import com.eva.workflow.approval.common.enums.RiskLevel;

public record HardRuleFlag(
        String code,
        RiskLevel level,
        String humanReadable
) {
}
