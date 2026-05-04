package com.eva.workflow.approval.api.dto.leave;

import java.time.Instant;

import com.eva.workflow.approval.common.enums.ActionType;

public record ApprovalActionResponse(
        Long id,
        Long actorId,
        String actorName,
        ActionType actionType,
        String comment,
        Instant createdAt
) {
}
