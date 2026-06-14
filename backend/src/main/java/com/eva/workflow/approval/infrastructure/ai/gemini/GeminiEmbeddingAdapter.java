package com.eva.workflow.approval.infrastructure.ai.gemini;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.eva.workflow.approval.domain.policy.service.EmbeddingPort;
import com.eva.workflow.approval.domain.policy.service.EmbeddingTaskType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Embeds text via Gemini's {@code text-embedding-004} model. Only wired when a Gemini
 * API key is configured (same condition as {@link GeminiWebClientConfig}); tests provide
 * a deterministic offline {@link EmbeddingPort} instead.
 */
@Component
@ConditionalOnProperty(prefix = "app.ai.gemini", name = "api-key")
public class GeminiEmbeddingAdapter implements EmbeddingPort {

    private static final int DIMENSION = 768;
    private static final String API_PATH = "/v1beta/models/{model}:embedContent";

    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient geminiWebClient;

    public GeminiEmbeddingAdapter(
            GeminiProperties properties,
            ObjectMapper objectMapper,
            @Qualifier("geminiWebClient") WebClient geminiWebClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.geminiWebClient = geminiWebClient;
    }

    @Override
    public float[] embed(String text) {
        return embed(text, null);
    }

    @Override
    public float[] embed(String text, EmbeddingTaskType taskType) {
        Map<String, Object> requestBody = buildRequestBody(text, taskType);

        String raw = geminiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(API_PATH)
                        .queryParam("key", properties.apiKey())
                        .build(properties.embeddingModel()))
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .block();

        return parseEmbedding(raw);
    }

    @Override
    public int dimension() {
        return DIMENSION;
    }

    @Override
    public String modelName() {
        return properties.embeddingModel();
    }

    Map<String, Object> buildRequestBody(String text, EmbeddingTaskType taskType) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", "models/" + properties.embeddingModel());
        body.put("content", Map.of("parts", List.of(Map.of("text", text))));
        if (taskType != null) {
            body.put("taskType", taskType.name());
        }
        return body;
    }

    float[] parseEmbedding(String raw) {
        try {
            JsonNode values = objectMapper.readTree(raw).path("embedding").path("values");
            if (!values.isArray() || values.isEmpty()) {
                throw new IllegalArgumentException("Missing embedding values in Gemini response");
            }
            float[] vector = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vector[i] = (float) values.get(i).asDouble();
            }
            return vector;
        } catch (Exception e) {
            throw new RuntimeException("AI_EMBEDDING_PARSE_ERROR", e);
        }
    }
}
