package com.eva.workflow.approval.infrastructure.ai.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

class GeminiAiReviewAdapterTest {

    private GeminiAiReviewAdapter adapter;

    private static final String VALID_RESPONSE = """
            {
              "candidates": [{
                "content": {
                  "parts": [{
                    "text": "{\\"summary\\": \\"Annual leave request for 1 workday with no risk flags.\\", \\"riskLevel\\": \\"LOW\\", \\"riskReasons\\": [], \\"recommendation\\": \\"APPROVE\\", \\"recommendationReason\\": \\"No issues detected.\\"}"
                  }]
                }
              }],
              "usageMetadata": {
                "promptTokenCount": 100,
                "candidatesTokenCount": 50,
                "totalTokenCount": 150
              }
            }
            """;

    private static final String MEDIUM_RISK_RESPONSE = """
            {
              "candidates": [{
                "content": {
                  "parts": [{
                    "text": "{\\"summary\\": \\"Frequent leave pattern detected.\\", \\"riskLevel\\": \\"MEDIUM\\", \\"riskReasons\\": [\\"5 leaves in 30 days\\"], \\"recommendation\\": \\"REVIEW_CAREFULLY\\", \\"recommendationReason\\": \\"Pattern warrants review.\\"}"
                  }]
                }
              }],
              "usageMetadata": {
                "promptTokenCount": 140,
                "candidatesTokenCount": 60,
                "totalTokenCount": 200
              }
            }
            """;

    @BeforeEach
    void setUp() {
        GeminiProperties props = new GeminiProperties("test-api-key", "gemini-2.0-flash-lite", 30);
        adapter = new GeminiAiReviewAdapter(props, new ObjectMapper(), mock(WebClient.class));
    }

    @Test
    void parseResponse_splits_input_and_output_tokens() {
        AiReviewResult result = adapter.parseResponse(VALID_RESPONSE, 450);

        assertThat(result.inputTokens()).isEqualTo(100);
        assertThat(result.outputTokens()).isEqualTo(50);
        assertThat(result.tokenUsage()).isEqualTo(150);
        assertThat(result.latencyMs()).isEqualTo(450);
    }

    @Test
    void parseResponse_emits_single_attempt_marked_not_fallback() {
        AiReviewResult result = adapter.parseResponse(VALID_RESPONSE, 450);

        assertThat(result.isFallback()).isFalse();
        assertThat(result.attempts()).hasSize(1);
        AiReviewAttempt attempt = result.attempts().get(0);
        assertThat(attempt.provider()).isEqualTo(AiProvider.GEMINI);
        assertThat(attempt.modelName()).isEqualTo("gemini-2.0-flash-lite");
        assertThat(attempt.latencyMs()).isEqualTo(450);
        assertThat(attempt.success()).isTrue();
        assertThat(attempt.errorMessage()).isNull();
    }

    @Test
    void parseResponse_uses_prompt_version_hash() {
        AiReviewResult result = adapter.parseResponse(VALID_RESPONSE, 450);

        assertThat(result.promptVersion()).isEqualTo(AiReviewPromptBuilder.PROMPT_VERSION_HASH);
        assertThat(result.provider()).isEqualTo(AiProvider.GEMINI);
        assertThat(result.modelName()).isEqualTo("gemini-2.0-flash-lite");
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(result.recommendation()).isEqualTo(AiRecommendation.APPROVE);
        assertThat(result.summary()).contains("Annual leave");
        assertThat(result.riskReasons()).isEmpty();
    }

    @Test
    void parseResponse_medium_risk_review_carefully() {
        AiReviewResult result = adapter.parseResponse(MEDIUM_RISK_RESPONSE, 600);

        assertThat(result.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(result.riskReasons()).containsExactly("5 leaves in 30 days");
        assertThat(result.recommendation()).isEqualTo(AiRecommendation.REVIEW_CAREFULLY);
        assertThat(result.inputTokens()).isEqualTo(140);
        assertThat(result.outputTokens()).isEqualTo(60);
        assertThat(result.tokenUsage()).isEqualTo(200);
    }

    @Test
    void parseResponse_throws_on_malformed_json_content() {
        String malformedResponse = """
                {
                  "candidates": [{
                    "content": { "parts": [{ "text": "not-valid-json" }] }
                  }]
                }
                """;

        assertThatThrownBy(() -> adapter.parseResponse(malformedResponse, 100))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("AI_PARSE_ERROR");
    }
}
