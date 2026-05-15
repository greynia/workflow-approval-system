package com.eva.workflow.approval.application.aireview;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.eva.workflow.approval.common.enums.AiProvider;
import com.eva.workflow.approval.common.enums.AiRecommendation;
import com.eva.workflow.approval.common.enums.AiReviewErrorCode;
import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.common.enums.RiskLevel;
import com.eva.workflow.approval.domain.aireview.model.AiReviewAttempt;
import com.eva.workflow.approval.domain.aireview.model.AiReviewResult;
import com.eva.workflow.approval.domain.aireview.model.HardRuleFlag;
import com.eva.workflow.approval.domain.aireview.model.ReviewSnapshot;
import com.eva.workflow.approval.domain.aireview.service.AiHardRuleEngine;
import com.eva.workflow.approval.domain.aireview.service.AiReviewPort;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.AiReviewEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.AiReviewRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiReviewOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AiReviewOrchestrator.class);
    private static final int WORK_MINUTES_PER_DAY = 480;
    private static final String RULE_ENGINE_MODEL = "workflow-rule-engine";

    private final ReviewSnapshotAssembler snapshotAssembler;
    private final AiHardRuleEngine hardRuleEngine;
    private final AiReviewPort aiReviewPort;
    private final AiReviewRepository aiReviewRepository;
    private final ObjectMapper objectMapper;

    public void review(Long requestId) {
        review(requestId, "en");
    }

    public void review(Long requestId, String locale) {
        AiReviewEntity entity = AiReviewEntity.createPending(requestId);
        aiReviewRepository.save(entity);

        ReviewSnapshot snapshot;
        try {
            snapshot = snapshotAssembler.assemble(requestId);
        } catch (Exception e) {
            markFailed(entity, requestId, AiReviewErrorCode.SNAPSHOT_BUILD_FAILED, null, e);
            return;
        }

        List<HardRuleFlag> flags;
        try {
            flags = hardRuleEngine.evaluate(snapshot, locale);
        } catch (Exception e) {
            markFailed(entity, requestId, AiReviewErrorCode.RULE_ENGINE_FAILED, AiProvider.RULE_ENGINE, e);
            return;
        }

        try {
            ProviderReview providerReview = flags.isEmpty()
                    ? new ProviderReview(buildLowRiskResult(snapshot, locale), null)
                    : reviewWithFallback(snapshot, flags, locale, requestId);
            AiReviewResult finalResult = enforceHardRuleFloor(providerReview.result(), flags);

            entity.markCompleted(
                    finalResult.summary(),
                    finalResult.riskLevel(),
                    toJson(finalResult.riskReasons()),
                    finalResult.recommendation(),
                    finalResult.recommendationReason(),
                    toJson(flags),
                    toJson(snapshot),
                    providerReview.rawAiResultJson(),
                    finalResult.modelName(),
                    finalResult.promptVersion(),
                    finalResult.provider(),
                    finalResult.inputTokens(),
                    finalResult.outputTokens(),
                    finalResult.tokenUsage(),
                    finalResult.latencyMs(),
                    finalResult.isFallback(),
                    toJson(finalResult.attempts())
            );
            aiReviewRepository.save(entity);
            log.info("AI review completed for requestId={} risk={}", requestId, finalResult.riskLevel());

        } catch (Exception e) {
            markFailed(entity, requestId, AiReviewErrorCode.INTERNAL_ERROR, null, e);
        }
    }

    private void markFailed(
            AiReviewEntity entity,
            Long requestId,
            AiReviewErrorCode code,
            AiProvider provider,
            Exception e
    ) {
        log.error("AI review failed for requestId={} code={}: {}", requestId, code, e.getMessage());
        entity.markFailed(code, provider);
        aiReviewRepository.save(entity);
    }

    private ProviderReview reviewWithFallback(
            ReviewSnapshot snapshot,
            List<HardRuleFlag> flags,
            String locale,
            Long requestId
    ) {
        try {
            AiReviewResult result = aiReviewPort.review(snapshot, flags, locale);
            return new ProviderReview(result, toJson(result));
        } catch (Exception e) {
            log.warn("AI provider failed for requestId={}, using deterministic fallback: {}", requestId, e.getMessage());
            return new ProviderReview(buildHardRuleFallbackResult(snapshot, flags, locale), null);
        }
    }

    private AiReviewResult enforceHardRuleFloor(AiReviewResult result, List<HardRuleFlag> flags) {
        RiskLevel hardRuleRiskLevel = hardRuleEngine.highestRiskLevel(flags);
        RiskLevel riskLevel = max(result.riskLevel(), hardRuleRiskLevel);
        List<String> riskReasons = mergeRiskReasons(result.riskReasons(), flags, riskLevel);
        AiRecommendation recommendation = result.recommendation();

        if (riskLevel == RiskLevel.HIGH && recommendation == AiRecommendation.APPROVE) {
            recommendation = AiRecommendation.REVIEW_CAREFULLY;
        }

        return new AiReviewResult(
                result.summary(),
                riskLevel,
                riskReasons,
                recommendation,
                result.recommendationReason(),
                result.modelName(),
                result.promptVersion(),
                result.provider(),
                result.inputTokens(),
                result.outputTokens(),
                result.tokenUsage(),
                result.latencyMs(),
                result.isFallback(),
                result.attempts()
        );
    }

    private AiReviewResult buildLowRiskResult(ReviewSnapshot snapshot, String locale) {
        boolean zh = isTraditionalChinese(locale);
        String duration = formatDuration(snapshot.durationMinutes(), zh);
        String leaveType = formatLeaveType(snapshot.leaveType(), zh);
        List<AiReviewAttempt> attempts = List.of(ruleEngineAttempt());

        if (zh) {
            return new AiReviewResult(
                    "此申請為" + leaveType + " " + duration + "，未命中需額外提醒的風險規則。",
                    RiskLevel.LOW,
                    List.of(),
                    AiRecommendation.APPROVE,
                    "系統未偵測到額外風險，可依一般請假流程核准。",
                    RULE_ENGINE_MODEL,
                    "rule-low-v1",
                    AiProvider.RULE_ENGINE,
                    0,
                    0,
                    0,
                    0,
                    false,
                    attempts
            );
        }

        return new AiReviewResult(
                "This is a " + duration + " " + leaveType + " request with no additional risk rules triggered.",
                RiskLevel.LOW,
                List.of(),
                AiRecommendation.APPROVE,
                "No additional risk rules were detected, so the request can proceed through the standard approval flow.",
                RULE_ENGINE_MODEL,
                "rule-low-v1",
                AiProvider.RULE_ENGINE,
                0,
                0,
                0,
                0,
                false,
                attempts
        );
    }

    private AiReviewAttempt ruleEngineAttempt() {
        return new AiReviewAttempt(AiProvider.RULE_ENGINE, RULE_ENGINE_MODEL, 0, true, null);
    }

    private AiReviewResult buildHardRuleFallbackResult(
            ReviewSnapshot snapshot,
            List<HardRuleFlag> flags,
            String locale
    ) {
        boolean zh = isTraditionalChinese(locale);
        RiskLevel riskLevel = hardRuleEngine.highestRiskLevel(flags);
        AiRecommendation recommendation = riskLevel == RiskLevel.LOW
                ? AiRecommendation.APPROVE
                : AiRecommendation.REVIEW_CAREFULLY;
        List<String> riskReasons = flags.stream()
                .map(HardRuleFlag::humanReadable)
                .toList();

        List<AiReviewAttempt> attempts = List.of(ruleEngineAttempt());

        if (zh) {
            return new AiReviewResult(
                    "此申請命中需注意的請假規則，AI 文字分析暫時不可用，以下依系統規則產生審核摘要。",
                    riskLevel,
                    riskReasons,
                    recommendation,
                    "系統已保留規則判斷，建議主管依注意事項審慎評估後再處理。",
                    RULE_ENGINE_MODEL,
                    "rule-fallback-v1",
                    AiProvider.RULE_ENGINE,
                    0,
                    0,
                    0,
                    0,
                    false,
                    attempts
            );
        }

        return new AiReviewResult(
                "This request matched leave review rules. AI text analysis is temporarily unavailable, so this review was generated from system rules.",
                riskLevel,
                riskReasons,
                recommendation,
                "The system preserved the rule-based assessment; the approver should review the listed risk flags before taking action.",
                RULE_ENGINE_MODEL,
                "rule-fallback-v1",
                AiProvider.RULE_ENGINE,
                0,
                0,
                0,
                0,
                false,
                attempts
        );
    }

    private boolean isTraditionalChinese(String locale) {
        return locale != null && locale.toLowerCase().startsWith("zh");
    }

    private String formatLeaveType(LeaveType leaveType, boolean zh) {
        if (zh) {
            return switch (leaveType) {
                case ANNUAL -> "特休";
                case SICK -> "病假";
                case PERSONAL -> "事假";
                case OTHER -> "其他假";
            };
        }

        return switch (leaveType) {
            case ANNUAL -> "annual leave";
            case SICK -> "sick leave";
            case PERSONAL -> "personal leave";
            case OTHER -> "other leave";
        };
    }

    private String formatDuration(int durationMinutes, boolean zh) {
        int fullDays = durationMinutes / WORK_MINUTES_PER_DAY;
        int remainingMinutes = durationMinutes % WORK_MINUTES_PER_DAY;
        int hours = remainingMinutes / 60;
        int minutes = remainingMinutes % 60;

        if (zh) {
            if (remainingMinutes == 0) {
                return fullDays + " 個工作天";
            }
            StringBuilder label = new StringBuilder();
            if (fullDays > 0) {
                label.append(fullDays).append(" 個工作天");
            }
            if (hours > 0) {
                if (!label.isEmpty()) {
                    label.append("又");
                }
                label.append(hours).append(" 小時");
            }
            if (minutes > 0) {
                if (!label.isEmpty()) {
                    label.append(" ");
                }
                label.append(minutes).append(" 分鐘");
            }
            return label.toString();
        }

        if (remainingMinutes == 0) {
            return fullDays == 1 ? "1 workday" : fullDays + " workdays";
        }
        StringBuilder label = new StringBuilder();
        if (fullDays > 0) {
            label.append(fullDays == 1 ? "1 workday" : fullDays + " workdays");
        }
        if (hours > 0) {
            if (!label.isEmpty()) {
                label.append(" and ");
            }
            label.append(hours == 1 ? "1 hour" : hours + " hours");
        }
        if (minutes > 0) {
            if (!label.isEmpty()) {
                label.append(" ");
            }
            label.append(minutes == 1 ? "1 minute" : minutes + " minutes");
        }
        return label.toString();
    }

    private RiskLevel max(RiskLevel first, RiskLevel second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.compareTo(second) >= 0 ? first : second;
    }

    private List<String> mergeRiskReasons(List<String> aiReasons, List<HardRuleFlag> flags, RiskLevel riskLevel) {
        if (flags.isEmpty() && riskLevel == RiskLevel.LOW) {
            return List.of();
        }

        Set<String> seen = new LinkedHashSet<>();
        List<String> reasons = new java.util.ArrayList<>();

        flags.stream()
                .map(HardRuleFlag::humanReadable)
                .forEach(reason -> addRiskReason(reasons, seen, reason));
        if (aiReasons != null) {
            aiReasons.forEach(reason -> addRiskReason(reasons, seen, reason));
        }
        return List.copyOf(reasons);
    }

    private void addRiskReason(List<String> reasons, Set<String> seen, String reason) {
        if (reason == null || reason.isBlank()) {
            return;
        }
        String normalized = normalizeRiskReason(reason);
        if (seen.add(normalized)) {
            reasons.add(reason);
        }
    }

    private String normalizeRiskReason(String reason) {
        return reason
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\p{Punct}]+", "");
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private record ProviderReview(AiReviewResult result, String rawAiResultJson) {
    }
}
