package com.eva.workflow.approval.domain.approval.model;

public record ApprovalFlowStep(
        int stepOrder,
        Long approverId
) {
}
