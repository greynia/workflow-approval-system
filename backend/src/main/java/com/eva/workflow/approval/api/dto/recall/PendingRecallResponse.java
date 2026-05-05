package com.eva.workflow.approval.api.dto.recall;

import java.time.LocalDateTime;

import com.eva.workflow.approval.common.enums.LeaveType;

public record PendingRecallResponse(
        Long stepId,
        Long requestId,
        Long applicantId,
        String applicantName,
        LeaveType leaveType,
        Integer durationMinutes,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String recallReason
) {
}
