package com.eva.workflow.approval.domain.approval.model;

import com.eva.workflow.approval.common.enums.ApprovalStepType;

public record ApprovalFlowStep(
        int stepOrder,
        Long approverId,
        ApprovalStepType stepType
) {
}
