package com.eva.workflow.approval.api.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InitYearBalancesRequest(
        @NotNull @Min(2020) @Max(2100) Integer year
) {}
