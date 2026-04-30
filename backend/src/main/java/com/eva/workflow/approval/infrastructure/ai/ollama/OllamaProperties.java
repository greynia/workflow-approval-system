package com.eva.workflow.approval.infrastructure.ai.ollama;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.ollama")
public record OllamaProperties(
        String baseUrl,
        String model,
        int timeoutSeconds
) {
}
