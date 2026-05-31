package com.eva.workflow.approval.application.policy;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tuning for Policy RAG ingestion and search.
 *
 * @param autoIngest        re-index the policy doc into pgvector on startup
 * @param chunkMaxChars     max characters per chunk before recursive sub-splitting kicks in
 * @param chunkOverlapChars characters of overlap between adjacent sub-chunks
 * @param candidateTopK     how many candidates to pull from vector search before reranking
 * @param finalTopK         how many matches to return after rerank + minScore filtering
 * @param minScore          drop reranked matches scoring below this (only applied when reranking)
 */
@ConfigurationProperties(prefix = "app.ai.policy")
public record PolicyProperties(
        boolean autoIngest,
        int chunkMaxChars,
        int chunkOverlapChars,
        int candidateTopK,
        int finalTopK,
        double minScore
) {
    public PolicyProperties {
        if (chunkMaxChars <= 0) {
            chunkMaxChars = 600;
        }
        if (chunkOverlapChars < 0) {
            chunkOverlapChars = 80;
        }
        if (candidateTopK <= 0) {
            candidateTopK = 25;
        }
        if (finalTopK <= 0) {
            finalTopK = 5;
        }
        if (minScore < 0) {
            minScore = 0;
        }
    }
}
