package com.eva.workflow.approval.api.dto.common;

public record ErrorResponse(
        String code,
        String message,
        String traceId
) {
}
