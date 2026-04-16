package com.eva.workflow.approval.domain.approval.model;

import com.eva.workflow.approval.common.enums.ApproverType;

public record ApprovalRule(
        Integer minMinutes,
        Integer maxMinutes,
        ApproverType approverType,
        int stepOrder
) {

    public boolean matches(int durationMinutes) {
        if (minMinutes != null && durationMinutes < minMinutes) {
            return false;
        }
        if (maxMinutes != null && durationMinutes > maxMinutes) {
            return false;
        }
        return true;
    }
}
