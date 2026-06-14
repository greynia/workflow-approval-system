package com.eva.workflow.approval.domain.policy.service;

import java.util.List;

/**
 * Second-stage relevance scorer (cross-encoder reranker). Given a query and a set of
 * candidate documents already retrieved by vector search, it re-scores each candidate by
 * reading query and document together, yielding far more precise ranking than the
 * bi-encoder cosine score alone.
 *
 * <p>Optional: when no reranker is configured the policy search falls back to plain
 * vector ordering, so callers inject this as an {@code Optional}.
 */
public interface RerankPort {

    /**
     * Relevance score in {@code [0, 1]} for each document against {@code query}, returned
     * in the same order as {@code documents}. Higher is more relevant.
     */
    List<Double> scoreAll(String query, List<String> documents);
}
