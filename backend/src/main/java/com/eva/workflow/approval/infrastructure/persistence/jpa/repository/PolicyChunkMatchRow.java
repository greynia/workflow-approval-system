package com.eva.workflow.approval.infrastructure.persistence.jpa.repository;

/**
 * Closed projection for a similarity-search hit. {@code score} is cosine similarity
 * in [0, 1] (1 - cosine distance); higher is more relevant.
 */
public interface PolicyChunkMatchRow {
    String getSection();

    String getSource();

    int getChunkIndex();

    String getContent();

    double getScore();
}
