package com.eva.workflow.approval.api.dto.auth;

import com.eva.workflow.approval.common.enums.UserRole;
import java.util.List;

public record LoginResponse(
        Long employeeId,
        String name,
        UserRole role,
        List<String> permissions,
        String preferredLocale
) {
}
