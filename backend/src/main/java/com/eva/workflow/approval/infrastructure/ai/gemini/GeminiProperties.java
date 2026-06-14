package com.eva.workflow.approval.infrastructure.ai.gemini;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.gemini")
public record GeminiProperties(
        String apiKey,
        String model,
        String embeddingModel,
        int timeoutSeconds
) {
}
