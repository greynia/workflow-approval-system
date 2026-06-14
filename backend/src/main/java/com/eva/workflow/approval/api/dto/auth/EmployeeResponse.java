package com.eva.workflow.approval.api.dto.auth;

import com.eva.workflow.approval.common.enums.UserRole;
import java.util.List;

public record EmployeeResponse(
        Long id,
        String employeeNo,
        String name,
        String email,
        UserRole role,
        Long departmentId,
        Long managerId,
        List<String> permissions
) {
}
