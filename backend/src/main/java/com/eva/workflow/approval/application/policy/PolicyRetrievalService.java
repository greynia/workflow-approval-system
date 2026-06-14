package com.eva.workflow.approval.application.policy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.domain.policy.service.EmbeddingPort;
import com.eva.workflow.approval.domain.policy.service.EmbeddingTaskType;
import com.eva.workflow.approval.domain.policy.service.RerankPort;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.PolicyChunkMatchRow;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.PolicyChunkRepository;

import lombok.RequiredArgsConstructor;

/**
 * Core two-stage policy retrieval, free of authorization/audit and any request-scoped actor so it
 * can be reused both by the AI-facing {@code PolicySearchService} endpoint and by the async
 * {@code AiReviewOrchestrator} (which runs after-commit with no SecurityContext).
 *
 * <p>Stage 1 embeds the query ({@code RETRIEVAL_QUERY}) and pulls {@code candidateTopK} nearest
 * chunks by pgvector cosine distance (recall). Stage 2, when a reranker is configured, re-scores
 * the candidates, drops those below {@code minScore}, and keeps {@code finalTopK} (precision).
 * Without a reranker it degrades to plain vector ordering.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.ai.gemini", name = "api-key")
public class PolicyRetrievalService {

    private final EmbeddingPort embeddingPort;
    private final Optional<RerankPort> rerankPort;
    private final PolicyChunkRepository policyChunkRepository;
    private final PolicyProperties properties;

    @Transactional(readOnly = true)
    public PolicyRetrieval retrieve(String query, String locale, Integer topKParam) {
        int finalTopK = clampFinalTopK(topKParam);
        int candidateTopK = Math.max(properties.candidateTopK(), finalTopK);

        float[] queryVector = embeddingPort.embed(query, EmbeddingTaskType.RETRIEVAL_QUERY);
        List<PolicyChunkMatchRow> candidates =
                policyChunkRepository.searchSimilar(toVectorLiteral(queryVector), locale, candidateTopK);

        boolean reranked = rerankPort.isPresent() && !candidates.isEmpty();
        List<PolicyMatch> matches = reranked
                ? rerank(rerankPort.get(), query, candidates, finalTopK)
                : candidates.stream().limit(finalTopK).map(this::toMatch).toList();

        return new PolicyRetrieval(reranked, finalTopK, candidates.size(), matches);
    }

    /** Re-scores candidates with the reranker, drops those below {@code minScore}, keeps top finalTopK. */
    private List<PolicyMatch> rerank(
            RerankPort port, String query, List<PolicyChunkMatchRow> candidates, int finalTopK) {
        List<String> documents = candidates.stream()
                .map(row -> row.getSection() + "\n" + row.getContent())
                .toList();
        List<Double> scores = port.scoreAll(query, documents);

        List<Scored> scored = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            double score = i < scores.size() ? scores.get(i) : 0.0;
            scored.add(new Scored(candidates.get(i), score));
        }
        return scored.stream()
                .filter(s -> s.score() >= properties.minScore())
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .limit(finalTopK)
                .map(s -> new PolicyMatch(
                        s.row().getSection(), s.row().getSource(), s.row().getChunkIndex(),
                        s.row().getContent(), s.score()))
                .toList();
    }

    private PolicyMatch toMatch(PolicyChunkMatchRow row) {
        return new PolicyMatch(
                row.getSection(), row.getSource(), row.getChunkIndex(), row.getContent(), row.getScore());
    }

    private int clampFinalTopK(Integer topK) {
        if (topK == null) {
            return properties.finalTopK();
        }
        if (topK < 1) {
            return 1;
        }
        return Math.min(topK, properties.finalTopK());
    }

    private record Scored(PolicyChunkMatchRow row, double score) {
    }

    /** Formats a vector as a pgvector literal: {@code [0.1,0.2,...]}. */
    private String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 8);
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }
}
