package com.eva.workflow.approval.infrastructure.ai.ollama;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import com.eva.workflow.approval.common.enums.AiProvider;
import com.eva.workflow.approval.common.enums.AiRecommendation;
import com.eva.workflow.approval.common.enums.RiskLevel;
import com.eva.workflow.approval.domain.aireview.model.AiReviewAttempt;
import com.eva.workflow.approval.domain.aireview.model.AiReviewResult;
import com.eva.workflow.approval.domain.aireview.model.HardRuleFlag;
import com.eva.workflow.approval.domain.aireview.model.ReviewSnapshot;
import com.eva.workflow.approval.domain.aireview.service.AiReviewPort;
import com.eva.workflow.approval.infrastructure.ai.AiReviewJsonParser;
import com.eva.workflow.approval.infrastructure.ai.PromptTemplateResolver;
import com.eva.workflow.approval.infrastructure.ai.RenderedPrompt;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OllamaAiReviewAdapter implements AiReviewPort {

    private static final Logger log = LoggerFactory.getLogger(OllamaAiReviewAdapter.class);

    private final OllamaProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient ollamaWebClient;
    private final PromptTemplateResolver promptTemplateResolver;

    @Override
    public AiProvider provider() {
        return AiProvider.LOCAL;
    }

    @Override
    public String modelName() {
        return properties.model();
    }

    @Override
    public AiReviewResult review(
            ReviewSnapshot snapshot, List<HardRuleFlag> flags, String locale, String policyContext) {
        long start = System.currentTimeMillis();
        RenderedPrompt rendered =
                promptTemplateResolver.resolveAndRender(snapshot, flags, locale, provider(), policyContext);

        Map<String, Object> requestBody = Map.of(
                "model", properties.model(),
                "messages", List.of(Map.of("role", "user", "content", rendered.text())),
                "stream", false,
                "format", "json"
        );

        String raw = ollamaWebClient.post()
                .uri("/api/chat")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .block();

        int latencyMs = (int) (System.currentTimeMillis() - start);
        return parseResponse(raw, latencyMs, rendered);
    }

    AiReviewResult parseResponse(String raw, int latencyMs, RenderedPrompt rendered) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            String content = root.path("message").path("content").asText();
            if (content == null || content.isBlank()) {
                throw new IllegalArgumentException("Missing message content");
            }
            JsonNode result = objectMapper.readTree(content);

            int inputTokens = root.path("prompt_eval_count").asInt(0);
            int outputTokens = root.path("eval_count").asInt(0);
            int tokenUsage = inputTokens + outputTokens;

            AiReviewAttempt attempt = new AiReviewAttempt(
                    AiProvider.LOCAL, properties.model(), latencyMs, true, null);

            return new AiReviewResult(
                    AiReviewJsonParser.requiredText(result, "summary", AiReviewJsonParser.MAX_SUMMARY_LENGTH),
                    AiReviewJsonParser.parseEnum(result, "riskLevel", RiskLevel.class),
                    AiReviewJsonParser.parseRiskReasons(result.path("riskReasons")),
                    AiReviewJsonParser.parseEnum(result, "recommendation", AiRecommendation.class),
                    AiReviewJsonParser.requiredText(result, "recommendationReason", AiReviewJsonParser.MAX_RECOMMENDATION_REASON_LENGTH),
                    properties.model(),
                    rendered.version(),
                    rendered.templateId(),
                    AiProvider.LOCAL,
                    inputTokens,
                    outputTokens,
                    tokenUsage,
                    latencyMs,
                    false,
                    List.of(attempt),
                    List.of()
            );
        } catch (Exception e) {
            log.warn("Failed to parse Ollama response: {}", e.getMessage());
            throw new RuntimeException("AI_PARSE_ERROR");
        }
    }
}
