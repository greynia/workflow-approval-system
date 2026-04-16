package com.eva.workflow.approval.api.dto.auth;

import com.eva.workflow.approval.common.enums.UserRole;

public record LoginResponse(
        Long employeeId,
        String name,
        UserRole role
) {
}
