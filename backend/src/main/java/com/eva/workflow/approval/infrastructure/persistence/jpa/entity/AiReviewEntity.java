package com.eva.workflow.approval.infrastructure.persistence.jpa.entity;

import java.time.LocalDateTime;

import com.eva.workflow.approval.common.enums.AiProvider;
import com.eva.workflow.approval.common.enums.AiRecommendation;
import com.eva.workflow.approval.common.enums.AiReviewErrorCode;
import com.eva.workflow.approval.common.enums.AiReviewStatus;
import com.eva.workflow.approval.common.enums.RiskLevel;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "ai_reviews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiReviewStatus status;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 10)
    private RiskLevel riskLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "risk_reasons_json", columnDefinition = "jsonb")
    private String riskReasonsJson;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private AiRecommendation recommendation;

    @Column(name = "recommendation_reason", columnDefinition = "TEXT")
    private String recommendationReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hard_rule_flags_json", columnDefinition = "jsonb")
    private String hardRuleFlagsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_snapshot_json", columnDefinition = "jsonb")
    private String inputSnapshotJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_ai_result_json", columnDefinition = "jsonb")
    private String rawAiResultJson;

    @Column(name = "model_name", length = 80)
    private String modelName;

    @Column(name = "prompt_version", length = 20)
    private String promptVersion;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AiProvider provider;

    @Column(name = "token_usage")
    private Integer tokenUsage;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "error_code", length = 60)
    private String errorCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static AiReviewEntity createPending(Long requestId) {
        AiReviewEntity entity = new AiReviewEntity();
        LocalDateTime now = LocalDateTime.now();
        entity.requestId = requestId;
        entity.status = AiReviewStatus.PENDING;
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    public void markCompleted(
            String summary,
            RiskLevel riskLevel,
            String riskReasonsJson,
            AiRecommendation recommendation,
            String recommendationReason,
            String hardRuleFlagsJson,
            String inputSnapshotJson,
            String rawAiResultJson,
            String modelName,
            String promptVersion,
            AiProvider provider,
            Integer tokenUsage,
            Integer latencyMs
    ) {
        this.status = AiReviewStatus.COMPLETED;
        this.summary = summary;
        this.riskLevel = riskLevel;
        this.riskReasonsJson = riskReasonsJson;
        this.recommendation = recommendation;
        this.recommendationReason = recommendationReason;
        this.hardRuleFlagsJson = hardRuleFlagsJson;
        this.inputSnapshotJson = inputSnapshotJson;
        this.rawAiResultJson = rawAiResultJson;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.provider = provider;
        this.tokenUsage = tokenUsage;
        this.latencyMs = latencyMs;
    }

    public void markFailed(AiReviewErrorCode errorCode, AiProvider provider) {
        this.status = AiReviewStatus.FAILED;
        this.errorCode = errorCode == null ? null : errorCode.name();
        this.provider = provider;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
