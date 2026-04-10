package com.eva.workflow.approval.api.dto.leave;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.common.enums.RequestStatus;

public record LeaveRequestDetailResponse(
        Long id,
        Long applicantId,
        String applicantName,
        Long deputyId,
        String deputyName,
        LeaveType type,
        LocalDate startDate,
        LocalDate endDate,
        Integer days,
        String reason,
        RequestStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ApprovalStepResponse> approvalSteps,
        List<ApprovalActionResponse> approvalActions
) {
}
