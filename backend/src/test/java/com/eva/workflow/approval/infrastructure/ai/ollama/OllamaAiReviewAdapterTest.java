package com.eva.workflow.approval.infrastructure.ai.ollama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.eva.workflow.approval.common.enums.AiProvider;
import com.eva.workflow.approval.common.enums.AiRecommendation;
import com.eva.workflow.approval.common.enums.RiskLevel;
import com.eva.workflow.approval.domain.aireview.model.AiReviewAttempt;
import com.eva.workflow.approval.domain.aireview.model.AiReviewResult;
import com.eva.workflow.approval.infrastructure.ai.AiReviewPromptBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;

class OllamaAiReviewAdapterTest {

    private OllamaAiReviewAdapter adapter;

    private static final String VALID_RESPONSE = """
            {
              "message": {
                "content": "{\\"summary\\": \\"Routine annual leave.\\", \\"riskLevel\\": \\"LOW\\", \\"riskReasons\\": [], \\"recommendation\\": \\"APPROVE\\", \\"recommendationReason\\": \\"No concerns.\\"}"
              },
              "prompt_eval_count": 120,
              "eval_count": 80
            }
            """;

    @BeforeEach
    void setUp() {
        OllamaProperties props = new OllamaProperties("http://localhost:11434", "llama3.1", 30);
        adapter = new OllamaAiReviewAdapter(props, new ObjectMapper(), mock(WebClient.class));
    }

    @Test
    void parseResponse_splits_input_and_output_tokens() {
        AiReviewResult result = adapter.parseResponse(VALID_RESPONSE, 750);

        assertThat(result.inputTokens()).isEqualTo(120);
        assertThat(result.outputTokens()).isEqualTo(80);
        assertThat(result.tokenUsage()).isEqualTo(200);
        assertThat(result.latencyMs()).isEqualTo(750);
    }

    @Test
    void parseResponse_emits_single_attempt_marked_not_fallback() {
        AiReviewResult result = adapter.parseResponse(VALID_RESPONSE, 750);

        assertThat(result.isFallback()).isFalse();
        assertThat(result.attempts()).hasSize(1);
        AiReviewAttempt attempt = result.attempts().get(0);
        assertThat(attempt.provider()).isEqualTo(AiProvider.LOCAL);
        assertThat(attempt.modelName()).isEqualTo("llama3.1");
        assertThat(attempt.latencyMs()).isEqualTo(750);
        assertThat(attempt.success()).isTrue();
        assertThat(attempt.errorMessage()).isNull();
    }

    @Test
    void parseResponse_uses_prompt_version_hash() {
        AiReviewResult result = adapter.parseResponse(VALID_RESPONSE, 750);

        assertThat(result.promptVersion()).isEqualTo(AiReviewPromptBuilder.PROMPT_VERSION_HASH);
        assertThat(result.provider()).isEqualTo(AiProvider.LOCAL);
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(result.recommendation()).isEqualTo(AiRecommendation.APPROVE);
    }
}
