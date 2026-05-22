package com.eva.workflow.approval.domain.policy.service;

/**
 * Turns text into a dense embedding vector. The same implementation must be used
 * for both ingestion and query, otherwise the vectors live in different spaces and
 * similarity search is meaningless.
 */
public interface EmbeddingPort {

    /** Embeds {@code text} into a {@link #dimension()}-length vector. */
    float[] embed(String text);

    /** Fixed dimensionality of every vector this port produces (e.g. 768). */
    int dimension();

    /** Identifier of the embedding model, stored with each chunk for provenance. */
    String modelName();
}
