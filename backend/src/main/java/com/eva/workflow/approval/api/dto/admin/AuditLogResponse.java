package com.eva.workflow.approval.api.dto.admin;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        String entityType,
        Long entityId,
        String action,
        Long actorId,
        String actorName,
        String detailJson,
        Instant createdAt
) {
}
