package com.eva.workflow.approval.domain.aireview.model;

/**
 * A company-policy clause the AI review consulted. {@code section}/{@code source}/{@code chunkIndex}
 * form a stable citation; {@code score} is the relevance score from retrieval.
 */
public record PolicyReference(
        String section,
        String source,
        int chunkIndex,
        String content,
        double score
) {
}
