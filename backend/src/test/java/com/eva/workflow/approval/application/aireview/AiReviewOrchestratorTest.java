package com.eva.workflow.approval.application.aireview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eva.workflow.approval.common.enums.AiProvider;
import com.eva.workflow.approval.common.enums.AiRecommendation;
import com.eva.workflow.approval.common.enums.AiReviewErrorCode;
import com.eva.workflow.approval.common.enums.AiReviewStatus;
import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.common.enums.RiskLevel;
import com.eva.workflow.approval.common.enums.UserRole;
import com.eva.workflow.approval.domain.aireview.model.HardRuleFlag;
import com.eva.workflow.approval.domain.aireview.model.AiReviewResult;
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
                objectMapper
        );
    }

    @Test
    void reviewDoesNotLetAiDowngradeHighHardRuleRisk() {
        ReviewSnapshot snapshot = snapshotWithNewHireRisk();
        when(snapshotAssembler.assemble(10L)).thenReturn(snapshot);
        when(aiReviewPort.review(any(), any(), any())).thenReturn(new AiReviewResult(
                "AI summary",
                RiskLevel.LOW,
                List.of("AI says low risk"),
                AiRecommendation.APPROVE,
                "AI recommends approval",
                "test-model",
                "v1",
                AiProvider.LOCAL,
                0,
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

        verify(aiReviewPort, never()).review(any(), any(), any());
        assertThat(completedReview.getRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(completedReview.getRecommendation()).isEqualTo(AiRecommendation.APPROVE);
        assertThat(completedReview.getRiskReasonsJson()).isEqualTo("[]");
        assertThat(completedReview.getSummary()).contains("特休 1 個工作天");
        assertThat(completedReview.getRecommendationReason()).contains("一般請假流程核准");
        assertThat(completedReview.getModelName()).isEqualTo("workflow-rule-engine");
        assertThat(completedReview.getPromptVersion()).isEqualTo("rule-low-v1");
        assertThat(completedReview.getProvider()).isEqualTo(AiProvider.RULE_ENGINE);
        assertThat(completedReview.getRawAiResultJson()).isNull();
    }

    @Test
    void reviewDeduplicatesAiRiskReasonAgainstLocalizedHardRuleReason() {
        ReviewSnapshot snapshot = snapshotWithNewHireShortLeaveRisk();
        when(snapshotAssembler.assemble(10L)).thenReturn(snapshot);
        when(aiReviewPort.review(any(), any(), any())).thenReturn(new AiReviewResult(
                "AI summary",
                RiskLevel.MEDIUM,
                List.of("新進員工在到職90天內申請請假"),
                AiRecommendation.REVIEW_CAREFULLY,
                "AI recommends careful review",
                "test-model",
                "v1",
                AiProvider.LOCAL,
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
        when(aiReviewPort.review(any(), any(), any())).thenThrow(new RuntimeException("timeout"));
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

        verify(aiReviewPort, never()).review(any(), any(), any());
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
                objectMapper
        );
        when(snapshotAssembler.assemble(10L)).thenReturn(snapshot);
        when(aiReviewRepository.save(any(AiReviewEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        orchestratorWithThrowingRuleEngine.review(10L, "zh-TW");

        org.mockito.Mockito.verify(aiReviewRepository, org.mockito.Mockito.times(2)).save(reviewCaptor.capture());
        List<AiReviewEntity> savedReviews = reviewCaptor.getAllValues();
        AiReviewEntity failedReview = savedReviews.get(savedReviews.size() - 1);

        verify(aiReviewPort, never()).review(any(), any(), any());
        assertThat(failedReview.getStatus()).isEqualTo(AiReviewStatus.FAILED);
        assertThat(failedReview.getErrorCode()).isEqualTo(AiReviewErrorCode.RULE_ENGINE_FAILED.name());
        assertThat(failedReview.getProvider()).isEqualTo(AiProvider.RULE_ENGINE);
    }

    private ReviewSnapshot snapshotWithNewHireRisk() {
        return new ReviewSnapshot(
                10L,
                LeaveType.ANNUAL,
                LocalDateTime.of(2026, 5, 7, 9, 0),
                LocalDateTime.of(2026, 5, 8, 18, 0),
                960,
                "New hire long leave request",
                LocalDateTime.of(2026, 4, 29, 10, 0),
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
                LocalDateTime.of(2026, 4, 29, 10, 0),
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
                LocalDateTime.of(2026, 4, 1, 9, 0),
                9L,
                35,
                UserRole.EMPLOYEE,
                "後端組",
                0,
                2
        );
    }
}
