package com.eva.workflow.approval.api.dto.aireview;

import java.util.List;

/**
 * Result of a semantic policy search, returned to the AI-facing caller.
 *
 * @param query   the original search text (echoed back)
 * @param topK    the effective number of results requested (after clamping)
 * @param matches relevant policy clauses, most similar first
 */
public record PolicySearchResponse(
        String query,
        int topK,
        List<Match> matches
) {

    /**
     * @param section heading of the policy clause, usable as a citation
     * @param content the clause text
     * @param score   cosine similarity in [0, 1]; higher is more relevant
     */
    public record Match(
            String section,
            String content,
            double score
    ) {
    }
}
