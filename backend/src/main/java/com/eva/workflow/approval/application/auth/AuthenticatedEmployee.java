package com.eva.workflow.approval.application.auth;

import com.eva.workflow.approval.common.enums.UserRole;
import java.util.List;

public record AuthenticatedEmployee(
        Long employeeId,
        String email,
        String name,
        UserRole role,
        List<String> permissions
) {
}
