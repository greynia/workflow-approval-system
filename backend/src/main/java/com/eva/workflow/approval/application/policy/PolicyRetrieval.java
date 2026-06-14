package com.eva.workflow.approval.application.policy;

import java.util.List;

/**
 * Outcome of a two-stage policy retrieval.
 *
 * @param reranked       whether a cross-encoder reranker scored the matches (false = plain vector order)
 * @param finalTopK      the effective number of results requested (after clamping)
 * @param candidateCount how many candidates the vector stage returned before rerank/minScore filtering
 * @param matches        the surviving matches, most relevant first
 */
public record PolicyRetrieval(
        boolean reranked,
        int finalTopK,
        int candidateCount,
        List<PolicyMatch> matches
) {
}
