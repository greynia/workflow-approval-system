package com.eva.workflow.approval.api.dto.admin;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateHolidayRequest(
        @NotNull LocalDate date,
        @NotBlank @Size(max = 100) String name
) {
}
