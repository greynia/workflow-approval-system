package com.eva.workflow.approval.api.dto.admin;

import java.time.Instant;

import com.eva.workflow.approval.common.enums.AiProvider;
import com.eva.workflow.approval.common.enums.PromptTemplateStatus;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.PromptTemplateEntity;

/**
 * Admin-facing view of a prompt template, including the full text so admins can
 * audit exactly what a given version says.
 */
public record PromptTemplateResponse(
        Long id,
        String name,
        String locale,
        AiProvider provider,
        String version,
        String templateText,
        PromptTemplateStatus status,
        Instant createdAt,
        Long createdBy
) {

    public static PromptTemplateResponse from(PromptTemplateEntity entity) {
        return new PromptTemplateResponse(
                entity.getId(),
                entity.getName(),
                entity.getLocale(),
                entity.getProvider(),
                entity.getVersion(),
                entity.getTemplateText(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getCreatedBy()
        );
    }
}
