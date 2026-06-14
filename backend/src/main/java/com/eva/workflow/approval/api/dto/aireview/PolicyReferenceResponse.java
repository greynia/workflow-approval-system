package com.eva.workflow.approval.api.dto.aireview;

/**
 * A company-policy clause cited by the AI review, returned to the approver UI as a citation.
 */
public record PolicyReferenceResponse(
        String section,
        String source,
        int chunkIndex,
        String content,
        double score
) {
}
