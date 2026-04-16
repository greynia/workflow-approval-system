package com.eva.workflow.approval.api.dto.leave;

import java.time.LocalDateTime;

import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.common.enums.LeaveRequestStage;
import com.eva.workflow.approval.common.enums.RequestStatus;

public record LeaveRequestSummaryResponse(
        Long id,
        LeaveType type,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer durationMinutes,
        RequestStatus status,
        LeaveRequestStage currentStage,
        LocalDateTime createdAt
) {
}
