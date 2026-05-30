package com.eva.workflow.approval.domain.policy.service;

/**
 * Retrieval role an embedding is produced for. Asymmetric-retrieval embedding models
 * (e.g. Gemini {@code text-embedding-004}) accept this as a hint so document and query
 * vectors land in better-aligned subspaces.
 */
public enum EmbeddingTaskType {
    RETRIEVAL_DOCUMENT,
    RETRIEVAL_QUERY
}
