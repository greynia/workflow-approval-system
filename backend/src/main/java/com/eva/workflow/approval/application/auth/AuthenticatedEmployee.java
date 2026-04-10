package com.eva.workflow.approval.application.auth;

import com.eva.workflow.approval.common.enums.UserRole;

public record AuthenticatedEmployee(
        Long employeeId,
        String email,
        String name,
        UserRole role
) {
}
