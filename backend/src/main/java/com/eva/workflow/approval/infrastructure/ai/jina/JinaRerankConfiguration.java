package com.eva.workflow.approval.infrastructure.ai.jina;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.eva.workflow.approval.domain.policy.service.RerankPort;

import dev.langchain4j.model.jina.JinaScoringModel;
import dev.langchain4j.model.scoring.ScoringModel;

/**
 * Wires the Jina reranker only when {@code app.ai.rerank.api-key} is set. Without it no
 * {@link RerankPort} bean exists, and policy search degrades to plain vector ordering.
 */
@Configuration
@EnableConfigurationProperties(RerankProperties.class)
@ConditionalOnProperty(prefix = "app.ai.rerank", name = "api-key")
public class JinaRerankConfiguration {

    @Bean
    RerankPort jinaRerankPort(RerankProperties properties) {
        ScoringModel scoringModel = JinaScoringModel.builder()
                .apiKey(properties.apiKey())
                .modelName(properties.model())
                .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .build();
        return new JinaRerankAdapter(scoringModel);
    }
}
