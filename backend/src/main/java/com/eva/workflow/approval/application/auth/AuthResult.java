package com.eva.workflow.approval.application.auth;

import com.eva.workflow.approval.common.enums.UserRole;

/**
 * Internal result from AuthService.login().
 * Contains both the JWT token (for cookie) and user info (for response body).
 */
public record AuthResult(
        String token,
        Long employeeId,
        String name,
        UserRole role
) {
}
