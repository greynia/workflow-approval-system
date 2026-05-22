package com.eva.workflow.approval;

import java.util.List;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.eva.workflow.approval.common.enums.AiProvider;
import com.eva.workflow.approval.common.enums.AiRecommendation;
import com.eva.workflow.approval.common.enums.RiskLevel;
import com.eva.workflow.approval.domain.aireview.model.AiReviewAttempt;
import com.eva.workflow.approval.domain.aireview.model.AiReviewResult;
import com.eva.workflow.approval.domain.aireview.service.AiReviewPort;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer<?> postgresContainer() {
		return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));
	}

	@Bean
	@Primary
	AiReviewPort testAiReviewPort() {
		return (snapshot, flags, locale) -> {
			AiReviewAttempt attempt = new AiReviewAttempt(
					AiProvider.LOCAL, "test-ai-review-port", 0, true, null);
			return new AiReviewResult(
					"Test AI review summary",
					RiskLevel.LOW,
					List.of(),
					AiRecommendation.APPROVE,
					"Test AI review recommendation",
					"test-ai-review-port",
					"test",
					null,
					AiProvider.LOCAL,
					0,
					0,
					0,
					0,
					false,
					List.of(attempt)
			);
		};
	}

}
