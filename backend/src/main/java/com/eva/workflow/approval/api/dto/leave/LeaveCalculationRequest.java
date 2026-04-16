package com.eva.workflow.approval.api.dto.leave;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

public record LeaveCalculationRequest(
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime
) {
}
