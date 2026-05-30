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
import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.common.enums.RiskLevel;
import com.eva.workflow.approval.common.enums.UserRole;
import com.eva.workflow.approval.domain.aireview.model.AiReviewAttempt;
import com.eva.workflow.approval.domain.aireview.model.AiReviewResult;
import com.eva.workflow.approval.domain.aireview.model.ReviewSnapshot;
import com.eva.workflow.approval.domain.aireview.service.AiReviewPort;

@ExtendWith(MockitoExtension.class)
class FallbackAiReviewAdapterTest {

    @Mock
    private AiReviewPort primary;

    @Mock
    private AiReviewPort fallback;

    private static AiReviewResult leafResult(AiProvider provider, String model, int latency) {
        AiReviewAttempt leafAttempt = new AiReviewAttempt(provider, model, latency, true, null);
        return new AiReviewResult(
                "leaf summary", RiskLevel.LOW, List.of(),
                AiRecommendation.APPROVE, "looks good",
                model, "promptv1", null, provider,
                40, 30, 70, latency, false, List.of(leafAttempt), List.of()
        );
    }

    @Test
    void primary_succeeds_records_one_attempt_no_fallback() {
        AiReviewResult primaryLeaf = leafResult(AiProvider.GEMINI, "gemini-test", 500);
        when(primary.review(any(), any(), any(), any())).thenReturn(primaryLeaf);
        FallbackAiReviewAdapter adapter = new FallbackAiReviewAdapter(List.of(primary, fallback));

        AiReviewResult result = adapter.review(snapshot(), List.of(), "en", "");

        assertThat(result.isFallback()).isFalse();
        assertThat(result.attempts()).hasSize(1);
        assertThat(result.attempts().get(0).provider()).isEqualTo(AiProvider.GEMINI);
        assertThat(result.attempts().get(0).success()).isTrue();
        assertThat(result.summary()).isEqualTo("leaf summary");
        verify(fallback, never()).review(any(), any(), any(), any());
    }

    @Test
    void primary_fails_fallback_succeeds_records_two_attempts_isFallback_true() {
        when(primary.review(any(), any(), any(), any())).thenThrow(new RuntimeException("primary timeout"));
        when(primary.provider()).thenReturn(AiProvider.GEMINI);
        when(primary.modelName()).thenReturn("gemini-test");
        AiReviewResult fallbackLeaf = leafResult(AiProvider.LOCAL, "ollama-test", 700);
        when(fallback.review(any(), any(), any(), any())).thenReturn(fallbackLeaf);
        FallbackAiReviewAdapter adapter = new FallbackAiReviewAdapter(List.of(primary, fallback));

        AiReviewResult result = adapter.review(snapshot(), List.of(), "en", "");

        assertThat(result.isFallback()).isTrue();
        assertThat(result.attempts()).hasSize(2);

        AiReviewAttempt failed = result.attempts().get(0);
        assertThat(failed.provider()).isEqualTo(AiProvider.GEMINI);
        assertThat(failed.modelName()).isEqualTo("gemini-test");
        assertThat(failed.success()).isFalse();
        assertThat(failed.errorMessage()).isEqualTo("primary timeout");

        AiReviewAttempt succeeded = result.attempts().get(1);
        assertThat(succeeded.provider()).isEqualTo(AiProvider.LOCAL);
        assertThat(succeeded.success()).isTrue();

        assertThat(result.latencyMs()).isEqualTo(failed.latencyMs() + succeeded.latencyMs());
        assertThat(result.summary()).isEqualTo("leaf summary");
    }

    @Test
    void all_adapters_fail_rethrows_last_exception() {
        RuntimeException lastException = new RuntimeException("all providers down");
        when(primary.review(any(), any(), any(), any())).thenThrow(new RuntimeException("primary down"));
        when(fallback.review(any(), any(), any(), any())).thenThrow(lastException);
        FallbackAiReviewAdapter adapter = new FallbackAiReviewAdapter(List.of(primary, fallback));

        assertThatThrownBy(() -> adapter.review(snapshot(), List.of(), "en", ""))
                .isEqualTo(lastException);
    }

    @Test
    void single_adapter_in_chain_still_wraps_with_one_attempt() {
        when(primary.review(any(), any(), any(), any())).thenReturn(leafResult(AiProvider.LOCAL, "ollama-test", 600));
        FallbackAiReviewAdapter adapter = new FallbackAiReviewAdapter(List.of(primary));

        AiReviewResult result = adapter.review(snapshot(), List.of(), "en", "");

        assertThat(result.isFallback()).isFalse();
        assertThat(result.attempts()).hasSize(1);
        assertThat(result.summary()).isEqualTo("leaf summary");
    }

    private ReviewSnapshot snapshot() {
        return new ReviewSnapshot(
                1L, LeaveType.ANNUAL,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now().plusHours(8),
                480, null, java.time.Instant.now(),
                10L, 365L,
                UserRole.EMPLOYEE,
                "Engineering", 0, 2
        );
    }

}
