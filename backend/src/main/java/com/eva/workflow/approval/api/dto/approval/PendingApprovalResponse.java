package com.eva.workflow.approval.api.dto.approval;

import java.time.Instant;
import java.time.LocalDateTime;

import com.eva.workflow.approval.common.enums.ApprovalStepType;
import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.common.enums.StepStatus;

public record PendingApprovalResponse(
        Long stepId,
        ApprovalStepType stepType,
        Long requestId,
        Long applicantId,
        String applicantName,
        LeaveType leaveType,
        Integer durationMinutes,
        LocalDateTime startTime,
        LocalDateTime endTime,
        StepStatus status,
        Instant createdAt
) {
}
