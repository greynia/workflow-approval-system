package com.eva.workflow.approval.infrastructure.persistence.jpa.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "audit_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(nullable = false, length = 50)
    private String action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private EmployeeEntity actor;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail_json", columnDefinition = "jsonb")
    private String detailJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static AuditLogEntity create(
            String entityType,
            Long entityId,
            String action,
            EmployeeEntity actor,
            String detailJson
    ) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.entityType = entityType;
        entity.entityId = entityId;
        entity.action = action;
        entity.actor = actor;
        entity.detailJson = detailJson;
        entity.createdAt = LocalDateTime.now();
        return entity;
    }
}
