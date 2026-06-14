package com.eva.workflow.approval.infrastructure.ai.jina;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.rerank")
public record RerankProperties(
        String apiKey,
        String model,
        int timeoutSeconds
) {
    public RerankProperties {
        if (model == null || model.isBlank()) {
            model = "jina-reranker-v2-base-multilingual";
        }
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 30;
        }
    }
}
