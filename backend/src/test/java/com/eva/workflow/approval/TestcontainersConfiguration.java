package com.eva.workflow.approval;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.eva.workflow.approval.domain.policy.service.EmbeddingPort;
import com.eva.workflow.approval.domain.policy.service.RerankPort;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	private static final int EMBEDDING_DIMENSION = 768;

	@Bean
	@ServiceConnection
	PostgreSQLContainer<?> postgresContainer() {
		// pgvector image so `CREATE EXTENSION vector` and vector(768) columns work.
		return new PostgreSQLContainer<>(
				DockerImageName.parse("pgvector/pgvector:pg16")
						.asCompatibleSubstituteFor("postgres"));
	}

	/**
	 * Deterministic, offline embedder for tests: a per-codepoint bag-of-characters vector.
	 * Works for CJK text too (Java's \w is not Unicode-aware), so texts that share
	 * characters land near each other under cosine similarity and real pgvector ordering
	 * can be asserted without calling Gemini.
	 */
	@Bean
	@Primary
	EmbeddingPort testEmbeddingPort() {
		return new EmbeddingPort() {
			@Override
			public float[] embed(String text) {
				float[] vector = new float[EMBEDDING_DIMENSION];
				if (text == null || text.isBlank()) {
					vector[0] = 1.0f;
					return vector;
				}
				text.codePoints().forEach(cp -> {
					if (!Character.isWhitespace(cp)) {
						vector[Math.floorMod(cp, EMBEDDING_DIMENSION)] += 1.0f;
					}
				});
				return vector;
			}

			@Override
			public int dimension() {
				return EMBEDDING_DIMENSION;
			}

			@Override
			public String modelName() {
				return "test-embedder";
			}
		};
	}

	/**
	 * Deterministic, offline reranker for tests: scores each document by how much of the query's
	 * character set it contains ({@code |query ∩ doc| / |query|}). CJK-safe, in [0,1], and high for
	 * documents sharing the query's characters — so relevant clauses clear the minScore floor while
	 * a no-overlap query scores ~0 and gets filtered out. Exercises the rerank path without Jina.
	 */
	@Bean
	@Primary
	RerankPort testRerankPort() {
		return (query, documents) -> {
			Set<Integer> queryChars = codepoints(query);
			return documents.stream()
					.map(doc -> {
						if (queryChars.isEmpty()) {
							return 0.0;
						}
						Set<Integer> docChars = codepoints(doc);
						long overlap = queryChars.stream().filter(docChars::contains).count();
						return (double) overlap / queryChars.size();
					})
					.toList();
		};
	}

	private static Set<Integer> codepoints(String text) {
		if (text == null) {
			return Set.of();
		}
		return text.codePoints()
				.filter(cp -> !Character.isWhitespace(cp))
				.boxed()
				.collect(Collectors.toSet());
	}

	@Bean
	@Primary
	AiReviewPort testAiReviewPort() {
		return (snapshot, flags, locale, policyContext) -> {
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
					List.of(attempt),
					List.of()
			);
		};
	}

}
