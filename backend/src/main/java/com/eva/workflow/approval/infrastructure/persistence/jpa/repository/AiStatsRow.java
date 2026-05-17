package com.eva.workflow.approval.infrastructure.persistence.jpa.repository;

import com.eva.workflow.approval.common.enums.AiProvider;
import com.eva.workflow.approval.common.enums.AiReviewStatus;

/**
 * Spring Data closed projection for the per-(provider, status) aggregate row
 * produced by {@link AiReviewRepository#aggregateBetween}. {@code provider} is
 * nullable — pre-provider failures (e.g. snapshot build) leave it null.
 */
public interface AiStatsRow {

    AiProvider getProvider();

    AiReviewStatus getStatus();

    long getCount();

    Double getAvgLatencyMs();

    long getLatencyCount();

    Long getTotalTokens();

    Long getTotalInputTokens();

    Long getTotalOutputTokens();

    Long getFallbackCount();
}
