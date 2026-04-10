package com.eva.workflow.approval.api.dto.approval;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.common.enums.StepStatus;

public record PendingApprovalResponse(
        Long stepId,
        Integer stepOrder,
        Long requestId,
        Long applicantId,
        String applicantName,
        LeaveType leaveType,
        Integer days,
        LocalDate startDate,
        LocalDate endDate,
        StepStatus status,
        LocalDateTime createdAt
) {
}
