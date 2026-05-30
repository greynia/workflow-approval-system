package com.eva.workflow.approval.domain.aireview.model;

import java.util.List;

import com.eva.workflow.approval.common.enums.AiProvider;
import com.eva.workflow.approval.common.enums.AiRecommendation;
import com.eva.workflow.approval.common.enums.RiskLevel;

public record AiReviewResult(
        String summary,
        RiskLevel riskLevel,
        List<String> riskReasons,
        AiRecommendation recommendation,
        String recommendationReason,
        String modelName,
        String promptVersion,
        Long promptTemplateId,
        AiProvider provider,
        int inputTokens,
        int outputTokens,
        int tokenUsage,
        int latencyMs,
        boolean isFallback,
        List<AiReviewAttempt> attempts,
        List<PolicyReference> policyReferences
) {
}
