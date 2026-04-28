package com.eva.workflow.approval.application.auth;

import java.time.LocalDateTime;

import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;

public record IssuedRefreshToken(
        EmployeeEntity employee,
        String token,
        LocalDateTime expiresAt
) {
}
