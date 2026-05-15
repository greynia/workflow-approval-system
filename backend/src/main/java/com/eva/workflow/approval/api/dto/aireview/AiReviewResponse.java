package com.eva.workflow.approval.api.dto.aireview;

import java.time.Instant;
import java.util.List;

import com.eva.workflow.approval.common.enums.AiProvider;
import com.eva.workflow.approval.common.enums.AiRecommendation;
import com.eva.workflow.approval.common.enums.AiReviewStatus;
import com.eva.workflow.approval.common.enums.RiskLevel;

public record AiReviewResponse(
        AiReviewStatus status,
        String summary,
        RiskLevel riskLevel,
        List<String> riskReasons,
        List<HardRuleFlagResponse> hardRuleFlags,
        AiRecommendation recommendation,
        String recommendationReason,
        String modelName,
        String promptVersion,
        AiProvider provider,
        Integer inputTokens,
        Integer outputTokens,
        Integer tokenUsage,
        Integer latencyMs,
        Boolean isFallback,
        String errorCode,
        Instant createdAt
) {
}
