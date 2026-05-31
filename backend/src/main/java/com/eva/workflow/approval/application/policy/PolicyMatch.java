package com.eva.workflow.approval.application.policy;

/**
 * A single retrieved policy chunk with its relevance score. {@code score} is the rerank score
 * when reranking ran, otherwise the cosine similarity. {@code section}/{@code source}/
 * {@code chunkIndex} together form a stable citation.
 */
public record PolicyMatch(
        String section,
        String source,
        int chunkIndex,
        String content,
        double score
) {
}
