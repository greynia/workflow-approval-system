package com.eva.workflow.approval.api.dto.admin;

import java.time.Instant;
import java.util.List;

import com.eva.workflow.approval.common.enums.AiProvider;

/**
 * Recent AI review problems for the admin dashboard, split into two buckets:
 *
 * <ul>
 *   <li>{@code hardFailures} — reviews persisted with status FAILED (snapshot /
 *       rule-engine / orchestrator errors). Their {@code attempts_json} is
 *       typically null, so {@code errorMessage} is usually null.</li>
 *   <li>{@code degradedReviews} — reviews that completed but only after the
 *       provider chain fell back ({@code is_fallback = true}). This is where a
 *       real LLM provider failure surfaces, with the failed provider's
 *       {@code errorMessage} extracted from {@code attempts_json}.</li>
 * </ul>
 */
public record RecentFailuresResponse(
        int limit,
        List<RecentFailureEntry> hardFailures,
        List<RecentFailureEntry> degradedReviews
) {

    public record RecentFailureEntry(
            long reviewId,
            long requestId,
            AiProvider provider,
            String errorCode,
            String errorMessage,
            Instant createdAt,
            boolean isFallback
    ) {
    }
}
