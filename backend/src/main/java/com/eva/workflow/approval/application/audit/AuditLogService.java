package com.eva.workflow.approval.application.audit;

import org.springframework.stereotype.Service;

import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.AuditLogEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public void log(String entityType, Long entityId, String action, EmployeeEntity actor, Object detail) {
        auditLogRepository.save(AuditLogEntity.create(
                entityType,
                entityId,
                action,
                actor,
                serializeDetail(detail)
        ));
    }

    private String serializeDetail(Object detail) {
        if (detail == null) {
            return null;
        }
        if (detail instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize audit detail", exception);
        }
    }
}
