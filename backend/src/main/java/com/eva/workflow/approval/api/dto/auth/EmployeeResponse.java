package com.eva.workflow.approval.api.dto.auth;

import com.eva.workflow.approval.common.enums.UserRole;

public record EmployeeResponse(
        Long id,
        String employeeNo,
        String name,
        String email,
        UserRole role,
        Long departmentId,
        Long managerId
) {
}
