package com.eva.workflow.approval.infrastructure.ai.gemini;

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
public class GeminiAiReviewAdapter implements AiReviewPort {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiReviewAdapter.class);
    private static final String API_PATH = "/v1beta/models/{model}:generateContent";

    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient geminiWebClient;
    private final PromptTemplateResolver promptTemplateResolver;

    @Override
    public AiProvider provider() {
        return AiProvider.GEMINI;
    }

    @Override
    public String modelName() {
        return properties.model();
    }

    @Override
    public AiReviewResult review(ReviewSnapshot snapshot, List<HardRuleFlag> flags, String locale) {
        long start = System.currentTimeMillis();
        RenderedPrompt rendered = promptTemplateResolver.resolveAndRender(snapshot, flags, locale, provider());

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", rendered.text())))),
                "generationConfig", Map.of("responseMimeType", "application/json")
        );

        String raw = geminiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(API_PATH)
                        .queryParam("key", properties.apiKey())
                        .build(properties.model()))
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
            String content = root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            if (content == null || content.isBlank()) {
                throw new IllegalArgumentException("Missing content in Gemini response");
            }
            JsonNode result = objectMapper.readTree(content);

            JsonNode usage = root.path("usageMetadata");
            int inputTokens = usage.path("promptTokenCount").asInt(0);
            int outputTokens = usage.path("candidatesTokenCount").asInt(0);
            int tokenUsage = inputTokens + outputTokens;

            AiReviewAttempt attempt = new AiReviewAttempt(
                    AiProvider.GEMINI, properties.model(), latencyMs, true, null);

            return new AiReviewResult(
                    AiReviewJsonParser.requiredText(result, "summary", AiReviewJsonParser.MAX_SUMMARY_LENGTH),
                    AiReviewJsonParser.parseEnum(result, "riskLevel", RiskLevel.class),
                    AiReviewJsonParser.parseRiskReasons(result.path("riskReasons")),
                    AiReviewJsonParser.parseEnum(result, "recommendation", AiRecommendation.class),
                    AiReviewJsonParser.requiredText(result, "recommendationReason", AiReviewJsonParser.MAX_RECOMMENDATION_REASON_LENGTH),
                    properties.model(),
                    rendered.version(),
                    rendered.templateId(),
                    AiProvider.GEMINI,
                    inputTokens,
                    outputTokens,
                    tokenUsage,
                    latencyMs,
                    false,
                    List.of(attempt)
            );
        } catch (Exception e) {
            log.warn("Failed to parse Gemini response: {}", e.getMessage());
            throw new RuntimeException("AI_PARSE_ERROR");
        }
    }
}
