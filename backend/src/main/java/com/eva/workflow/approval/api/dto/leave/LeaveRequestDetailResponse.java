package com.eva.workflow.approval.api.dto.leave;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.common.enums.LeaveRequestStage;
import com.eva.workflow.approval.common.enums.RequestStatus;

public record LeaveRequestDetailResponse(
        Long id,
        Long applicantId,
        String applicantName,
        Long deputyId,
        String deputyName,
        LeaveType type,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer durationMinutes,
        String reason,
        RequestStatus status,
        LeaveRequestStage currentStage,
        Instant createdAt,
        Instant updatedAt,
        List<ApprovalStepResponse> approvalSteps,
        List<ApprovalActionResponse> approvalActions
) {
}
