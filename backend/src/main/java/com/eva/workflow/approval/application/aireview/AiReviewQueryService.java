package com.eva.workflow.approval.application.aireview;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.api.dto.aireview.AiReviewResponse;
import com.eva.workflow.approval.api.dto.aireview.HardRuleFlagResponse;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.application.exception.ForbiddenApplicationException;
import com.eva.workflow.approval.application.exception.ResourceNotFoundApplicationException;
import com.eva.workflow.approval.application.request.LeaveRequestApplicationService;
import com.eva.workflow.approval.common.Permission;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.AiReviewEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.AiReviewRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiReviewQueryService {

    private final AiReviewRepository aiReviewRepository;
    private final ObjectMapper objectMapper;
    private final LeaveRequestApplicationService leaveRequestApplicationService;

    @Transactional(readOnly = true)
    public AiReviewResponse getAiReview(AuthenticatedEmployee caller, Long requestId) {
        if (!caller.permissions().contains(Permission.APPROVAL_VIEW)) {
            throw new ForbiddenApplicationException("AI review access forbidden");
        }
        leaveRequestApplicationService.assertCanAccessRequest(caller, requestId);

        AiReviewEntity entity = aiReviewRepository.findByRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundApplicationException("AI review not found"));

        return toResponse(entity);
    }

    private AiReviewResponse toResponse(AiReviewEntity entity) {
        return new AiReviewResponse(
                entity.getStatus(),
                entity.getSummary(),
                entity.getRiskLevel(),
                parseRiskReasons(entity.getRiskReasonsJson()),
                parseHardRuleFlags(entity.getHardRuleFlagsJson()),
                entity.getRecommendation(),
                entity.getRecommendationReason(),
                entity.getModelName(),
                entity.getPromptVersion(),
                entity.getProvider(),
                entity.getInputTokens(),
                entity.getOutputTokens(),
                entity.getTokenUsage(),
                entity.getLatencyMs(),
                entity.isFallback(),
                entity.getErrorCode(),
                entity.getCreatedAt()
        );
    }

    private List<String> parseRiskReasons(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<HardRuleFlagResponse> parseHardRuleFlags(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<HardRuleFlagResponse>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
