package com.eva.workflow.approval.api.dto.admin;

import java.time.LocalDate;

public record HolidayResponse(
        Long id,
        LocalDate date,
        String name,
        Integer year
) {
}
