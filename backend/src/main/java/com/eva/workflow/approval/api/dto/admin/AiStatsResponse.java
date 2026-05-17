package com.eva.workflow.approval.api.dto.admin;

import java.time.Instant;
import java.util.Map;

import com.eva.workflow.approval.common.enums.AiProvider;

/**
 * Aggregate AI review statistics for the admin observability dashboard.
 *
 * <p>Top-level counters cover every review in the window; {@code byProvider}
 * only buckets rows that reached a provider (pre-provider failures have a null
 * provider and are excluded from the map but still counted in the totals).
 */
public record AiStatsResponse(
        Instant from,
        Instant to,
        long totalReviews,
        long completed,
        long failed,
        long fallbackCount,
        Map<AiProvider, ProviderStats> byProvider,
        String currentPromptVersion
) {

    public record ProviderStats(
            long count,
            long completed,
            long failed,
            double avgLatencyMs,
            long totalInputTokens,
            long totalOutputTokens,
            long totalTokens
    ) {
    }
}
