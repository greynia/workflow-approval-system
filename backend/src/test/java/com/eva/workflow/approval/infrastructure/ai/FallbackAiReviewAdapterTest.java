package com.eva.workflow.approval.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eva.workflow.approval.common.enums.AiProvider;
import com.eva.workflow.approval.common.enums.AiRecommendation;
import com.eva.workflow.approval.common.enums.RiskLevel;
import com.eva.workflow.approval.domain.aireview.model.AiReviewResult;
import com.eva.workflow.approval.domain.aireview.model.HardRuleFlag;
import com.eva.workflow.approval.domain.aireview.model.ReviewSnapshot;
import com.eva.workflow.approval.domain.aireview.service.AiReviewPort;

@ExtendWith(MockitoExtension.class)
class FallbackAiReviewAdapterTest {

    @Mock
    private AiReviewPort primary;

    @Mock
    private AiReviewPort fallback;

    private static final AiReviewResult PRIMARY_RESULT = new AiReviewResult(
            "primary summary", RiskLevel.LOW, List.of(),
            AiRecommendation.APPROVE, "looks good",
            "primary-model", "v1", AiProvider.GEMINI, 100, 500
    );

    private static final AiReviewResult FALLBACK_RESULT = new AiReviewResult(
            "fallback summary", RiskLevel.LOW, List.of(),
            AiRecommendation.APPROVE, "looks good",
            "fallback-model", "v2", AiProvider.LOCAL, 80, 600
    );

    @Test
    void primary_succeeds_no_fallback_called() {
        when(primary.review(any(), any(), any())).thenReturn(PRIMARY_RESULT);
        FallbackAiReviewAdapter adapter = new FallbackAiReviewAdapter(List.of(primary, fallback));

        AiReviewResult result = adapter.review(snapshot(), List.of(), "en");

        assertThat(result).isEqualTo(PRIMARY_RESULT);
        verify(fallback, never()).review(any(), any(), any());
    }

    @Test
    void primary_fails_fallback_returns_result() {
        when(primary.review(any(), any(), any())).thenThrow(new RuntimeException("timeout"));
        when(fallback.review(any(), any(), any())).thenReturn(FALLBACK_RESULT);
        FallbackAiReviewAdapter adapter = new FallbackAiReviewAdapter(List.of(primary, fallback));

        AiReviewResult result = adapter.review(snapshot(), List.of(), "en");

        assertThat(result).isEqualTo(FALLBACK_RESULT);
    }

    @Test
    void all_fail_rethrows_last_exception() {
        RuntimeException lastException = new RuntimeException("all providers down");
        when(primary.review(any(), any(), any())).thenThrow(new RuntimeException("primary down"));
        when(fallback.review(any(), any(), any())).thenThrow(lastException);
        FallbackAiReviewAdapter adapter = new FallbackAiReviewAdapter(List.of(primary, fallback));

        assertThatThrownBy(() -> adapter.review(snapshot(), List.of(), "en"))
                .isEqualTo(lastException);
    }

    @Test
    void single_adapter_in_chain_delegates_directly() {
        when(primary.review(any(), any(), any())).thenReturn(PRIMARY_RESULT);
        FallbackAiReviewAdapter adapter = new FallbackAiReviewAdapter(List.of(primary));

        AiReviewResult result = adapter.review(snapshot(), List.of(), "en");

        assertThat(result).isEqualTo(PRIMARY_RESULT);
    }

    private ReviewSnapshot snapshot() {
        return new ReviewSnapshot(
                1L, com.eva.workflow.approval.common.enums.LeaveType.ANNUAL,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now().plusHours(8),
                480, null, java.time.Instant.now(),
                10L, 365L,
                com.eva.workflow.approval.common.enums.UserRole.EMPLOYEE,
                "Engineering", 0, 2
        );
    }
}
