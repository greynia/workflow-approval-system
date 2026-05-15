package com.eva.workflow.approval.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
import com.eva.workflow.approval.infrastructure.ai.gemini.GeminiAiReviewAdapter;
import com.eva.workflow.approval.infrastructure.ai.ollama.OllamaAiReviewAdapter;

@ExtendWith(MockitoExtension.class)
class AiAdapterConfigurationTest {

    @Mock
    private OllamaAiReviewAdapter ollamaAdapter;

    @Mock
    private GeminiAiReviewAdapter geminiAdapter;

    private final AiAdapterConfiguration configuration = new AiAdapterConfiguration();

    @Test
    void local_without_gemini_key_uses_ollama_only() {
        AiReviewPort port = configuration.aiReviewPort(
                "LOCAL",
                true,
                Optional.empty(),
                ollamaAdapter
        );

        assertThat(port).isSameAs(ollamaAdapter);
    }

    @Test
    void local_with_gemini_key_falls_back_to_gemini_when_ollama_fails() {
        when(ollamaAdapter.review(any(), any(), any())).thenThrow(new RuntimeException("ollama down"));
        when(geminiAdapter.review(any(), any(), any())).thenReturn(geminiResult());

        AiReviewPort port = configuration.aiReviewPort(
                "LOCAL",
                true,
                Optional.of(geminiAdapter),
                ollamaAdapter
        );

        AiReviewResult result = port.review(snapshot(), List.of(), "en");

        assertThat(result.provider()).isEqualTo(AiProvider.GEMINI);
    }

    @Test
    void gemini_with_fallback_enabled_falls_back_to_ollama_when_gemini_fails() {
        when(geminiAdapter.review(any(), any(), any())).thenThrow(new RuntimeException("gemini down"));
        when(ollamaAdapter.review(any(), any(), any())).thenReturn(localResult());

        AiReviewPort port = configuration.aiReviewPort(
                "GEMINI",
                true,
                Optional.of(geminiAdapter),
                ollamaAdapter
        );

        AiReviewResult result = port.review(snapshot(), List.of(), "en");

        assertThat(result.provider()).isEqualTo(AiProvider.LOCAL);
    }

    @Test
    void gemini_without_api_key_fails_fast() {
        assertThatThrownBy(() -> configuration.aiReviewPort(
                "GEMINI",
                true,
                Optional.empty(),
                ollamaAdapter
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("app.ai.provider=GEMINI but app.ai.gemini.api-key is not configured");
    }

    @Test
    void gemini_with_fallback_disabled_uses_gemini_only() {
        when(geminiAdapter.review(any(), any(), any())).thenReturn(geminiResult());

        AiReviewPort port = configuration.aiReviewPort(
                "GEMINI",
                false,
                Optional.of(geminiAdapter),
                ollamaAdapter
        );

        AiReviewResult result = port.review(snapshot(), List.of(), "en");

        assertThat(result.provider()).isEqualTo(AiProvider.GEMINI);
        verify(ollamaAdapter, never()).review(any(), any(), any());
    }

    private AiReviewResult localResult() {
        AiReviewAttempt attempt = new AiReviewAttempt(AiProvider.LOCAL, "llama3.1:8b", 600, true, null);
        return new AiReviewResult(
                "local summary",
                RiskLevel.LOW,
                List.of(),
                AiRecommendation.APPROVE,
                "looks good",
                "llama3.1:8b",
                "v2",
                AiProvider.LOCAL,
                30,
                50,
                80,
                600,
                false,
                List.of(attempt)
        );
    }

    private AiReviewResult geminiResult() {
        AiReviewAttempt attempt = new AiReviewAttempt(AiProvider.GEMINI, "gemini-2.0-flash-lite", 500, true, null);
        return new AiReviewResult(
                "gemini summary",
                RiskLevel.LOW,
                List.of(),
                AiRecommendation.APPROVE,
                "looks good",
                "gemini-2.0-flash-lite",
                "v1",
                AiProvider.GEMINI,
                40,
                60,
                100,
                500,
                false,
                List.of(attempt)
        );
    }

    private ReviewSnapshot snapshot() {
        return new ReviewSnapshot(
                1L,
                LeaveType.ANNUAL,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(8),
                480,
                null,
                Instant.now(),
                10L,
                365L,
                UserRole.EMPLOYEE,
                "Engineering",
                0,
                2
        );
    }
}
