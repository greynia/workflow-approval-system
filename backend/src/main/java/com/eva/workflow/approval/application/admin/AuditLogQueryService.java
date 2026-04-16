package com.eva.workflow.approval.application.admin;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.api.dto.admin.AuditLogResponse;
import com.eva.workflow.approval.api.dto.common.PageResponse;
import com.eva.workflow.approval.application.exception.ForbiddenApplicationException;
import com.eva.workflow.approval.application.exception.BadRequestApplicationException;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.common.enums.UserRole;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.AuditLogEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.AuditLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getAuditLogs(
            AuthenticatedEmployee authenticatedEmployee,
            String entityType,
            String action,
            String actorName,
            LocalDateTime createdFrom,
            LocalDateTime createdTo,
            int page,
            int size
    ) {
        if (authenticatedEmployee.role() != UserRole.ADMIN) {
            throw new ForbiddenApplicationException("Only admins can access audit logs");
        }
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new BadRequestApplicationException("createdFrom must be earlier than or equal to createdTo");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<AuditLogEntity> specification = Specification.allOf(
                hasEntityType(entityType),
                hasAction(action),
                hasActorName(actorName),
                createdAtGte(createdFrom),
                createdAtLte(createdTo)
        );

        Page<AuditLogEntity> result = auditLogRepository.findAll(specification, pageable);

        return new PageResponse<>(
                result.getContent().stream()
                        .map(this::toResponse)
                        .toList(),
                result.getNumber(),
                result.getTotalElements(),
                result.getSize(),
                result.getTotalPages()
        );
    }

    private AuditLogResponse toResponse(AuditLogEntity entity) {
        return new AuditLogResponse(
                entity.getId(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getAction(),
                entity.getActor().getId(),
                entity.getActor().getName(),
                entity.getDetailJson(),
                entity.getCreatedAt()
        );
    }

    private Specification<AuditLogEntity> hasEntityType(String entityType) {
        String normalized = trimToNull(entityType);
        if (normalized == null) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("entityType"), normalized);
    }

    private Specification<AuditLogEntity> hasAction(String action) {
        String normalized = trimToNull(action);
        if (normalized == null) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("action"), normalized);
    }

    private Specification<AuditLogEntity> hasActorName(String actorName) {
        String normalized = trimToNull(actorName);
        if (normalized == null) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("actor").get("name")),
                        "%" + normalized.toLowerCase() + "%"
                );
    }

    private Specification<AuditLogEntity> createdAtGte(LocalDateTime createdFrom) {
        if (createdFrom == null) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
    }

    private Specification<AuditLogEntity> createdAtLte(LocalDateTime createdTo) {
        if (createdTo == null) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdTo);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
