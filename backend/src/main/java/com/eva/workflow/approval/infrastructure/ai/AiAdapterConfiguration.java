package com.eva.workflow.approval.infrastructure.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import com.eva.workflow.approval.domain.aireview.service.AiReviewPort;
import com.eva.workflow.approval.infrastructure.ai.gemini.GeminiAiReviewAdapter;
import com.eva.workflow.approval.infrastructure.ai.gemini.GeminiProperties;
import com.eva.workflow.approval.infrastructure.ai.ollama.OllamaAiReviewAdapter;
import com.eva.workflow.approval.infrastructure.ai.ollama.OllamaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(OllamaProperties.class)
public class AiAdapterConfiguration {

    @Bean
    public OllamaAiReviewAdapter ollamaAiReviewAdapter(
            OllamaProperties ollamaProperties,
            ObjectMapper objectMapper,
            WebClient ollamaWebClient,
            PromptTemplateResolver promptTemplateResolver) {
        return new OllamaAiReviewAdapter(ollamaProperties, objectMapper, ollamaWebClient, promptTemplateResolver);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.ai.gemini", name = "api-key")
    public GeminiAiReviewAdapter geminiAiReviewAdapter(
            GeminiProperties geminiProperties,
            ObjectMapper objectMapper,
            @Qualifier("geminiWebClient") WebClient geminiWebClient,
            PromptTemplateResolver promptTemplateResolver) {
        return new GeminiAiReviewAdapter(geminiProperties, objectMapper, geminiWebClient, promptTemplateResolver);
    }

    @Bean
    public AiReviewPort aiReviewPort(
            @Value("${app.ai.provider:LOCAL}") String primaryProvider,
            @Value("${app.ai.fallback-enabled:true}") boolean fallbackEnabled,
            Optional<GeminiAiReviewAdapter> geminiAdapter,
            OllamaAiReviewAdapter ollamaAdapter) {

        List<AiReviewPort> chain = new ArrayList<>();

        switch (primaryProvider.toUpperCase()) {
            case "GEMINI" -> {
                geminiAdapter.ifPresentOrElse(chain::add, () -> {
                    throw new IllegalStateException(
                            "app.ai.provider=GEMINI but app.ai.gemini.api-key is not configured");
                });
                if (fallbackEnabled) chain.add(ollamaAdapter);
            }
            case "LOCAL" -> {
                chain.add(ollamaAdapter);
                if (fallbackEnabled) {
                    geminiAdapter.ifPresent(chain::add);
                }
            }
            default -> throw new IllegalStateException("Unknown AI provider: " + primaryProvider);
        }

        return chain.size() == 1 ? chain.get(0) : new FallbackAiReviewAdapter(chain);
    }
}
