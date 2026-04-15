package com.eva.workflow.approval.api.dto.leave;

import java.time.LocalDateTime;

import com.eva.workflow.approval.common.enums.ApprovalStepType;
import com.eva.workflow.approval.common.enums.StepStatus;

public record ApprovalStepResponse(
        Long id,
        Long approverId,
        String approverName,
        ApprovalStepType stepType,
        StepStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
