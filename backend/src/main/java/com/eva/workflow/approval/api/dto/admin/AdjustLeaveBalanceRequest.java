package com.eva.workflow.approval.api.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdjustLeaveBalanceRequest(
        @NotNull @Min(0) Integer quotaMinutes,
        @NotBlank @Size(max = 200) String reason
) {}
