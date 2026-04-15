package com.eva.workflow.approval.api.dto.leave;

import java.time.LocalDateTime;

import com.eva.workflow.approval.common.enums.LeaveType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLeaveRequest(
        @NotNull LeaveType type,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        @Size(max = 1000) String reason,
        @NotNull Long deputyId
) {
}
