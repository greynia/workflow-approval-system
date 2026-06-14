package com.eva.workflow.approval.api.dto.admin;

import java.time.LocalDate;

public record EmployeeScheduleResponse(
        Long id,
        Long employeeId,
        String employeeName,
        String employeeNo,
        String scheduleType,
        LocalDate effectiveFrom,
        LocalDate effectiveTo
) {
}
