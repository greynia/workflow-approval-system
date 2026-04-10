package com.eva.workflow.approval.api.dto.leave;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.common.enums.RequestStatus;

public record LeaveRequestSummaryResponse(
        Long id,
        LeaveType type,
        LocalDate startDate,
        LocalDate endDate,
        Integer days,
        RequestStatus status,
        LocalDateTime createdAt
) {
}
