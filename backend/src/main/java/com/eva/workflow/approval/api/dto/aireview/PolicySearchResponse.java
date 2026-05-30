package com.eva.workflow.approval.api.dto.aireview;

import java.util.List;

/**
 * Result of a semantic policy search, returned to the AI-facing caller.
 *
 * @param query    the original search text (echoed back)
 * @param topK     the effective number of results requested (after clamping)
 * @param reranked whether a cross-encoder reranker scored the matches (false = plain vector order)
 * @param matches  relevant policy clauses, most relevant first
 */
public record PolicySearchResponse(
        String query,
        int topK,
        boolean reranked,
        List<Match> matches
) {

    /**
     * @param section heading of the policy clause, usable as a citation
     * @param source  origin document filename, usable as a citation
     * @param chunkIndex 0-based position within the section, usable as a stable citation suffix
     * @param content the clause text
     * @param score   relevance in [0, 1] (rerank score when reranked, else cosine similarity)
     */
    public record Match(
            String section,
            String source,
            int chunkIndex,
            String content,
            double score
    ) {
    }
}
