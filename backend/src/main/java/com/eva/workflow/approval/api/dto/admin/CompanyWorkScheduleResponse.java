package com.eva.workflow.approval.api.dto.admin;

import java.time.LocalDate;
import java.time.LocalTime;

public record CompanyWorkScheduleResponse(
        Long id,
        LocalTime workStart,
        LocalTime workEnd,
        LocalTime lunchStart,
        LocalTime lunchEnd,
        String workDays,
        LocalDate effectiveFrom
) {
}
