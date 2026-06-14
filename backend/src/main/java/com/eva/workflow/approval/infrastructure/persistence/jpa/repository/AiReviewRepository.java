package com.eva.workflow.approval.infrastructure.persistence.jpa.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eva.workflow.approval.common.enums.AiReviewStatus;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.AiReviewEntity;

public interface AiReviewRepository extends JpaRepository<AiReviewEntity, Long> {

    Optional<AiReviewEntity> findByRequestId(Long requestId);

    List<AiReviewEntity> findByStatus(AiReviewStatus status);

    @Query("""
            SELECT a.provider AS provider,
                   a.status AS status,
                   COUNT(a) AS count,
                   AVG(a.latencyMs) AS avgLatencyMs,
                   COUNT(a.latencyMs) AS latencyCount,
                   SUM(COALESCE(a.tokenUsage, 0)) AS totalTokens,
                   SUM(COALESCE(a.inputTokens, 0)) AS totalInputTokens,
                   SUM(COALESCE(a.outputTokens, 0)) AS totalOutputTokens,
                   SUM(CASE WHEN a.isFallback = TRUE THEN 1L ELSE 0L END) AS fallbackCount
            FROM AiReviewEntity a
            WHERE a.createdAt BETWEEN :from AND :to
            GROUP BY a.provider, a.status
            """)
    List<AiStatsRow> aggregateBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT a FROM AiReviewEntity a
            WHERE a.status = com.eva.workflow.approval.common.enums.AiReviewStatus.FAILED
            ORDER BY a.createdAt DESC
            """)
    List<AiReviewEntity> findRecentFailures(Pageable pageable);

    @Query("""
            SELECT a FROM AiReviewEntity a
            WHERE a.status = com.eva.workflow.approval.common.enums.AiReviewStatus.COMPLETED
              AND a.isFallback = TRUE
            ORDER BY a.createdAt DESC
            """)
    List<AiReviewEntity> findRecentDegraded(Pageable pageable);
}
