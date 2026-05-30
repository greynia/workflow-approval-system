package com.eva.workflow.approval.application.aireview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eva.workflow.approval.application.policy.PolicyMatch;
import com.eva.workflow.approval.application.policy.PolicyRetrieval;
import com.eva.workflow.approval.application.policy.PolicyRetrievalService;
import com.eva.workflow.approval.common.enums.AiProvider;
import com.eva.workflow.approval.common.enums.AiRecommendation;
import com.eva.workflow.approval.common.enums.AiReviewErrorCode;
import com.eva.workflow.approval.common.enums.AiReviewStatus;
import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.common.enums.RiskLevel;
import com.eva.workflow.approval.common.enums.UserRole;
import com.eva.workflow.approval.domain.aireview.model.AiReviewAttempt;
import com.eva.workflow.approval.domain.aireview.model.AiReviewResult;
import com.eva.workflow.approval.domain.aireview.model.HardRuleFlag;
import com.eva.workflow.approval.domain.aireview.model.ReviewSnapshot;
import com.eva.workflow.approval.domain.aireview.service.AiHardRuleEngine;
import com.eva.workflow.approval.domain.aireview.service.AiReviewPort;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.AiReviewEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.AiReviewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AiReviewOrchestratorTest {

    @Mock
    private ReviewSnapshotAssembler snapshotAssembler;

    private final AiHardRuleEngine hardRuleEngine = new AiHardRuleEngine();

    @Mock
    private AiReviewPort aiReviewPort;

    @Mock
    private AiReviewRepository aiReviewRepository;

    @Mock
    private PolicyRetrievalService policyRetrievalService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Captor
    private ArgumentCaptor<AiReviewEntity> reviewCaptor;

    private AiReviewOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new AiReviewOrchestrator(
                snapshotAssembler,
                hardRuleEngine,
                aiReviewPort,
                aiReviewRepository,
                objectMapper,
                Optional.empty()
        );
    }

    @Test
    void reviewDoesNotLetAiDowngradeHighHardRuleRisk() {
        ReviewSnapshot snapshot = snapshotWithNewHireRisk();
        when(snapshotAssembler.assemble(10L)).thenReturn(snapshot);
        when(aiReviewPort.review(any(), any(), any(), any())).thenReturn(leafResult(
                "AI summary",
                RiskLevel.LOW,
                List.of("AI says low risk"),
                AiRecommendation.APPROVE,
                "AI recommends approval",
                AiProvider.LOCAL,
                40,
                60,
                100
        ));
        when(aiReviewRepository.save(any(AiReviewEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.review(10L);

        org.mockito.Mockito.verify(aiReviewRepository, org.mockito.Mockito.times(2)).save(reviewCaptor.capture());
        List<AiReviewEntity> savedReviews = reviewCaptor.getAllValues();
        AiReviewEntity completedReview = savedReviews.get(savedReviews.size() - 1);

        assertThat(completedReview.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(completedReview.getRecommendation()).isEqualTo(AiRecommendation.REVIEW_CAREFULLY);
        assertThat(completedReview.getRawAiResultJson()).contains("AI summary");
        assertThat(completedReview.getRiskReasonsJson())
                .contains("AI says low risk")
                .contains("New hire requests more than 1 workday of leave");
        assertThat(completedReview.getInputTokens()).isEqualTo(40);
        assertThat(completedReview.getOutputTokens()).isEqualTo(60);
        assertThat(completedReview.getTokenUsage()).isEqualTo(100);
        assertThat(completedReview.getLatencyMs()).isEqualTo(100);
        assertThat(completedReview.isFallback()).isFalse();
        assertThat(completedReview.getAttemptsJson()).contains("LOCAL");
    }

    @Test
    void reviewUsesDeterministicLowRiskResultWithoutCallingAiWhenNoHardRuleFlags() {
        ReviewSnapshot snapshot = snapshotWithoutHardRuleRisk();
        when(snapshotAssembler.assemble(10L)).thenReturn(snapshot);
        when(aiReviewRepository.save(any(AiReviewEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.review(10L, "zh-TW");

        org.mockito.Mockito.verify(aiReviewRepository, org.mockito.Mockito.times(2)).save(reviewCaptor.capture());
        List<AiReviewEntity> savedReviews = reviewCaptor.getAllValues();
        AiReviewEntity completedReview = savedReviews.get(savedReviews.size() - 1);

        verify(aiReviewPort, never()).review(any(), any(), any(), any());
        assertThat(completedReview.getRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(completedReview.getRecommendation()).isEqualTo(AiRecommendation.APPROVE);
        assertThat(completedReview.getRiskReasonsJson()).isEqualTo("[]");
        assertThat(completedReview.getSummary()).contains("特休 1 個工作天");
        assertThat(completedReview.getRecommendationReason()).contains("一般請假流程核准");
        assertThat(completedReview.getModelName()).isEqualTo("workflow-rule-engine");
        assertThat(completedReview.getPromptVersion()).isEqualTo("rule-low-v1");
        assertThat(completedReview.getProvider()).isEqualTo(AiProvider.RULE_ENGINE);
        assertThat(completedReview.getRawAiResultJson()).isNull();
        assertThat(completedReview.getAttemptsJson()).contains("RULE_ENGINE");
        assertThat(completedReview.isFallback()).isFalse();
    }

    @Test
    void reviewDeduplicatesAiRiskReasonAgainstLocalizedHardRuleReason() {
        ReviewSnapshot snapshot = snapshotWithNewHireShortLeaveRisk();
        when(snapshotAssembler.assemble(10L)).thenReturn(snapshot);
        when(aiReviewPort.review(any(), any(), any(), any())).thenReturn(leafResult(
                "AI summary",
                RiskLevel.MEDIUM,
                List.of("新進員工在到職90天內申請請假"),
                AiRecommendation.REVIEW_CAREFULLY,
                "AI recommends careful review",
                AiProvider.LOCAL,
                0,
                0,
                100
        ));
        when(aiReviewRepository.save(any(AiReviewEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.review(10L, "zh-TW");

        org.mockito.Mockito.verify(aiReviewRepository, org.mockito.Mockito.times(2)).save(reviewCaptor.capture());
        List<AiReviewEntity> savedReviews = reviewCaptor.getAllValues();
        AiReviewEntity completedReview = savedReviews.get(savedReviews.size() - 1);

        assertThat(completedReview.getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(completedReview.getRiskReasonsJson())
                .isEqualTo("[\"新進員工在到職 90 天內申請請假\"]");
    }

    @Test
    void reviewCompletesWithRuleEngineFallbackWhenAiProviderFails() {
        ReviewSnapshot snapshot = snapshotWithNewHireShortLeaveRisk();
        when(snapshotAssembler.assemble(10L)).thenReturn(snapshot);
        when(aiReviewPort.review(any(), any(), any(), any())).thenThrow(new RuntimeException("timeout"));
        when(aiReviewRepository.save(any(AiReviewEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.review(10L, "zh-TW");

        org.mockito.Mockito.verify(aiReviewRepository, org.mockito.Mockito.times(2)).save(reviewCaptor.capture());
        List<AiReviewEntity> savedReviews = reviewCaptor.getAllValues();
        AiReviewEntity completedReview = savedReviews.get(savedReviews.size() - 1);

        assertThat(completedReview.getStatus().name()).isEqualTo("COMPLETED");
        assertThat(completedReview.getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(completedReview.getProvider()).isEqualTo(AiProvider.RULE_ENGINE);
        assertThat(completedReview.getPromptVersion()).isEqualTo("rule-fallback-v1");
        assertThat(completedReview.getRawAiResultJson()).isNull();
        assertThat(completedReview.isFallback()).isFalse();
        assertThat(completedReview.getAttemptsJson()).contains("RULE_ENGINE");
        assertThat(completedReview.getRiskReasonsJson())
                .isEqualTo("[\"新進員工在到職 90 天內申請請假\"]");
        assertThat(completedReview.getSummary()).contains("AI 文字分析暫時不可用");
    }

    @Test
    void reviewMarksSnapshotBuildFailureWithStructuredErrorCodeAndNoProvider() {
        when(snapshotAssembler.assemble(10L)).thenThrow(new RuntimeException("snapshot failed"));
        when(aiReviewRepository.save(any(AiReviewEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.review(10L, "zh-TW");

        org.mockito.Mockito.verify(aiReviewRepository, org.mockito.Mockito.times(2)).save(reviewCaptor.capture());
        List<AiReviewEntity> savedReviews = reviewCaptor.getAllValues();
        AiReviewEntity failedReview = savedReviews.get(savedReviews.size() - 1);

        verify(aiReviewPort, never()).review(any(), any(), any(), any());
        assertThat(failedReview.getStatus()).isEqualTo(AiReviewStatus.FAILED);
        assertThat(failedReview.getErrorCode()).isEqualTo(AiReviewErrorCode.SNAPSHOT_BUILD_FAILED.name());
        assertThat(failedReview.getProvider()).isNull();
    }

    @Test
    void reviewMarksRuleEngineFailureWithStructuredErrorCodeAndRuleEngineProvider() {
        ReviewSnapshot snapshot = snapshotWithNewHireShortLeaveRisk();
        AiHardRuleEngine throwingHardRuleEngine = new AiHardRuleEngine() {
            @Override
            public List<HardRuleFlag> evaluate(ReviewSnapshot snapshot, String locale) {
                throw new RuntimeException("rule failed");
            }
        };
        AiReviewOrchestrator orchestratorWithThrowingRuleEngine = new AiReviewOrchestrator(
                snapshotAssembler,
                throwingHardRuleEngine,
                aiReviewPort,
                aiReviewRepository,
                objectMapper,
                Optional.empty()
        );
        when(snapshotAssembler.assemble(10L)).thenReturn(snapshot);
        when(aiReviewRepository.save(any(AiReviewEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        orchestratorWithThrowingRuleEngine.review(10L, "zh-TW");

        org.mockito.Mockito.verify(aiReviewRepository, org.mockito.Mockito.times(2)).save(reviewCaptor.capture());
        List<AiReviewEntity> savedReviews = reviewCaptor.getAllValues();
        AiReviewEntity failedReview = savedReviews.get(savedReviews.size() - 1);

        verify(aiReviewPort, never()).review(any(), any(), any(), any());
        assertThat(failedReview.getStatus()).isEqualTo(AiReviewStatus.FAILED);
        assertThat(failedReview.getErrorCode()).isEqualTo(AiReviewErrorCode.RULE_ENGINE_FAILED.name());
        assertThat(failedReview.getProvider()).isEqualTo(AiProvider.RULE_ENGINE);
    }

    private AiReviewResult leafResult(
            String summary,
            RiskLevel riskLevel,
            List<String> riskReasons,
            AiRecommendation recommendation,
            String recommendationReason,
            AiProvider provider,
            int inputTokens,
            int outputTokens,
            int latencyMs
    ) {
        AiReviewAttempt attempt = new AiReviewAttempt(provider, "test-model", latencyMs, true, null);
        return new AiReviewResult(
                summary,
                riskLevel,
                riskReasons,
                recommendation,
                recommendationReason,
                "test-model",
                "v1",
                null,
                provider,
                inputTokens,
                outputTokens,
                inputTokens + outputTokens,
                latencyMs,
                false,
                List.of(attempt),
                List.of()
        );
    }

    @Test
    void reviewInjectsRetrievedPolicyContextAndPersistsReferences() {
        ReviewSnapshot snapshot = snapshotWithNewHireRisk();
        AiReviewOrchestrator policyAwareOrchestrator = new AiReviewOrchestrator(
                snapshotAssembler,
                hardRuleEngine,
                aiReviewPort,
                aiReviewRepository,
                objectMapper,
                Optional.of(policyRetrievalService)
        );
        when(snapshotAssembler.assemble(10L)).thenReturn(snapshot);
        when(policyRetrievalService.retrieve(any(), any(), any())).thenReturn(new PolicyRetrieval(
                true, 5, 12,
                List.of(new PolicyMatch(
                        "新進員工請假限制", "leave-policy.zh.md", 0,
                        "到職未滿九十日之新進員工請假將標記為高風險。", 0.9))));
        when(aiReviewPort.review(any(), any(), any(), any())).thenReturn(leafResult(
                "AI summary",
                RiskLevel.LOW,
                List.of(),
                AiRecommendation.APPROVE,
                "AI recommends approval",
                AiProvider.LOCAL,
                10,
                10,
                50
        ));
        when(aiReviewRepository.save(any(AiReviewEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        policyAwareOrchestrator.review(10L, "zh-TW");

        ArgumentCaptor<String> policyContextCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiReviewPort).review(any(), any(), any(), policyContextCaptor.capture());
        assertThat(policyContextCaptor.getValue())
                .contains("Company Policy Excerpts")
                .contains("新進員工請假限制");

        org.mockito.Mockito.verify(aiReviewRepository, org.mockito.Mockito.times(2)).save(reviewCaptor.capture());
        AiReviewEntity completedReview = reviewCaptor.getAllValues().get(1);
        assertThat(completedReview.getPolicyReferencesJson())
                .contains("新進員工請假限制")
                .contains("leave-policy.zh.md");
    }

    private ReviewSnapshot snapshotWithNewHireRisk() {
        return new ReviewSnapshot(
                10L,
                LeaveType.ANNUAL,
                LocalDateTime.of(2026, 5, 7, 9, 0),
                LocalDateTime.of(2026, 5, 8, 18, 0),
                960,
                "New hire long leave request",
                Instant.parse("2026-04-29T10:00:00Z"),
                7L,
                30,
                UserRole.EMPLOYEE,
                "後端組",
                0,
                2
        );
    }

    private ReviewSnapshot snapshotWithoutHardRuleRisk() {
        return new ReviewSnapshot(
                10L,
                LeaveType.ANNUAL,
                LocalDateTime.of(2026, 5, 7, 9, 0),
                LocalDateTime.of(2026, 5, 7, 18, 0),
                480,
                "Annual leave request",
                Instant.parse("2026-04-29T10:00:00Z"),
                6L,
                1034,
                UserRole.EMPLOYEE,
                "前端組",
                0,
                2
        );
    }

    private ReviewSnapshot snapshotWithNewHireShortLeaveRisk() {
        return new ReviewSnapshot(
                10L,
                LeaveType.PERSONAL,
                LocalDateTime.of(2026, 5, 6, 9, 0),
                LocalDateTime.of(2026, 5, 6, 18, 0),
                480,
                "Personal leave request",
                Instant.parse("2026-04-01T09:00:00Z"),
                9L,
                35,
                UserRole.EMPLOYEE,
                "後端組",
                0,
                2
        );
    }
}
