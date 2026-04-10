package com.eva.workflow.approval.api.dto.leave;

import java.time.LocalDate;

import com.eva.workflow.approval.common.enums.LeaveType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLeaveRequest(
        @NotNull LeaveType type,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull @Min(1) Integer days,
        @Size(max = 1000) String reason,
        Long deputyId
) {
}
