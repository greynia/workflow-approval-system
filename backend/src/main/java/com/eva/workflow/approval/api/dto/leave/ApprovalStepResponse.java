package com.eva.workflow.approval.api.dto.leave;

import java.time.LocalDateTime;

import com.eva.workflow.approval.common.enums.StepStatus;

public record ApprovalStepResponse(
        Long id,
        Integer stepOrder,
        Long approverId,
        String approverName,
        StepStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
