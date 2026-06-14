package com.eva.workflow.approval.domain.aireview.model;

import com.eva.workflow.approval.common.enums.AiProvider;

public record AiReviewAttempt(
        AiProvider provider,
        String modelName,
        int latencyMs,
        boolean success,
        String errorMessage
) {
}
