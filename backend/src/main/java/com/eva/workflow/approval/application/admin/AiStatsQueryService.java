package com.eva.workflow.approval.application.admin;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.api.dto.admin.AiStatsResponse;
import com.eva.workflow.approval.api.dto.admin.AiStatsResponse.ProviderStats;
import com.eva.workflow.approval.api.dto.admin.RecentFailuresResponse;
import com.eva.workflow.approval.api.dto.admin.RecentFailuresResponse.RecentFailureEntry;
import com.eva.workflow.approval.application.aireview.AiPromptVersionProvider;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.application.exception.BadRequestApplicationException;
import com.eva.workflow.approval.application.exception.ForbiddenApplicationException;
import com.eva.workflow.approval.common.enums.AiProvider;
import com.eva.workflow.approval.common.enums.AiReviewStatus;
import com.eva.workflow.approval.common.enums.UserRole;
import com.eva.workflow.approval.domain.aireview.model.AiReviewAttempt;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.AiReviewEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.AiReviewRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.AiStatsRow;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiStatsQueryService {

    private static final Logger log = LoggerFactory.getLogger(AiStatsQueryService.class);
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final Duration DEFAULT_WINDOW = Duration.ofDays(7);

    private final AiReviewRepository aiReviewRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final AiPromptVersionProvider promptVersionProvider;

    @Transactional(readOnly = true)
    public AiStatsResponse getStats(AuthenticatedEmployee actor, String fromParam, String toParam) {
        requireAdmin(actor);

        Instant now = clock.instant();
        Instant to = parseInstant(toParam, "to", now);
        Instant from = parseInstant(fromParam, "from", now.minus(DEFAULT_WINDOW));
        if (from.isAfter(to)) {
            throw new BadRequestApplicationException("'from' must not be after 'to'");
        }

        List<AiStatsRow> rows = aiReviewRepository.aggregateBetween(from, to);

        long totalReviews = 0;
        long completed = 0;
        long failed = 0;
        long fallbackCount = 0;
        Map<AiProvider, ProviderAccumulator> accumulators = new EnumMap<>(AiProvider.class);

        for (AiStatsRow row : rows) {
            long count = row.getCount();
            totalReviews += count;
            if (row.getStatus() == AiReviewStatus.COMPLETED) {
                completed += count;
            } else if (row.getStatus() == AiReviewStatus.FAILED) {
                failed += count;
            }
            fallbackCount += orZero(row.getFallbackCount());

            if (row.getProvider() != null) {
                accumulators.computeIfAbsent(row.getProvider(), p -> new ProviderAccumulator()).add(row);
            }
        }

        Map<AiProvider, ProviderStats> byProvider = new EnumMap<>(AiProvider.class);
        accumulators.forEach((provider, acc) -> byProvider.put(provider, acc.toStats()));

        return new AiStatsResponse(
                from,
                to,
                totalReviews,
                completed,
                failed,
                fallbackCount,
                byProvider,
                promptVersionProvider.currentPromptVersion()
        );
    }

    @Transactional(readOnly = true)
    public RecentFailuresResponse getRecentFailures(AuthenticatedEmployee actor, Integer limitParam) {
        requireAdmin(actor);

        int limit = clampLimit(limitParam);
        Pageable page = PageRequest.of(0, limit);

        List<RecentFailureEntry> hardFailures = aiReviewRepository.findRecentFailures(page).stream()
                .map(this::toFailureEntry)
                .toList();
        List<RecentFailureEntry> degradedReviews = aiReviewRepository.findRecentDegraded(page).stream()
                .map(this::toFailureEntry)
                .toList();

        return new RecentFailuresResponse(limit, hardFailures, degradedReviews);
    }

    private RecentFailureEntry toFailureEntry(AiReviewEntity entity) {
        return new RecentFailureEntry(
                entity.getId(),
                entity.getRequestId(),
                entity.getProvider(),
                entity.getErrorCode(),
                extractErrorMessage(entity.getId(), entity.getAttemptsJson()),
                entity.getCreatedAt(),
                entity.isFallback()
        );
    }

    /**
     * Pulls the error text of the last failed attempt out of {@code attempts_json}.
     * Hard failures persist a null column, so a null return is expected and normal.
     */
    private String extractErrorMessage(Long reviewId, String attemptsJson) {
        if (attemptsJson == null || attemptsJson.isBlank()) {
            return null;
        }
        try {
            List<AiReviewAttempt> attempts =
                    objectMapper.readValue(attemptsJson, new TypeReference<List<AiReviewAttempt>>() {});
            String message = null;
            for (AiReviewAttempt attempt : attempts) {
                if (!attempt.success() && attempt.errorMessage() != null) {
                    message = attempt.errorMessage();
                }
            }
            return message;
        } catch (JsonProcessingException e) {
            log.warn("Unable to parse AI review attempts_json for reviewId={}: {}", reviewId, e.getOriginalMessage());
            return null;
        }
    }

    private Instant parseInstant(String value, String field, Instant fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new BadRequestApplicationException(
                    "Invalid '" + field + "' timestamp; expected ISO-8601 instant");
        }
    }

    private int clampLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private void requireAdmin(AuthenticatedEmployee actor) {
        if (actor.role() != UserRole.ADMIN) {
            throw new ForbiddenApplicationException("Only admins can view AI review statistics");
        }
    }

    private static long orZero(Long value) {
        return value == null ? 0L : value;
    }

    /** Folds the per-(provider, status) rows of one provider into a single bucket. */
    private static final class ProviderAccumulator {
        private long count;
        private long completed;
        private long failed;
        private double latencyWeightedSum;
        private long latencyWeight;
        private long totalInputTokens;
        private long totalOutputTokens;
        private long totalTokens;

        void add(AiStatsRow row) {
            long rowCount = row.getCount();
            count += rowCount;
            if (row.getStatus() == AiReviewStatus.COMPLETED) {
                completed += rowCount;
            } else if (row.getStatus() == AiReviewStatus.FAILED) {
                failed += rowCount;
            }
            if (row.getAvgLatencyMs() != null) {
                long latencyCount = row.getLatencyCount();
                latencyWeightedSum += row.getAvgLatencyMs() * latencyCount;
                latencyWeight += latencyCount;
            }
            totalInputTokens += orZero(row.getTotalInputTokens());
            totalOutputTokens += orZero(row.getTotalOutputTokens());
            totalTokens += orZero(row.getTotalTokens());
        }

        ProviderStats toStats() {
            double avgLatency = latencyWeight == 0 ? 0.0 : latencyWeightedSum / latencyWeight;
            return new ProviderStats(
                    count,
                    completed,
                    failed,
                    avgLatency,
                    totalInputTokens,
                    totalOutputTokens,
                    totalTokens
            );
        }
    }
}
