package com.eva.workflow.approval.application.admin;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.api.dto.admin.PromptTemplateResponse;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.application.exception.ForbiddenApplicationException;
import com.eva.workflow.approval.application.exception.ResourceNotFoundApplicationException;
import com.eva.workflow.approval.common.enums.UserRole;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.PromptTemplateRepository;

import lombok.RequiredArgsConstructor;

/**
 * Read-only admin access to the prompt registry. Write operations (create / activate
 * / archive) are deferred; status swaps are done via migration/SQL for now.
 */
@Service
@RequiredArgsConstructor
public class PromptQueryService {

    /**
     * Hard cap on the read-only listing. Templates are immutable history, so the
     * table only grows; this bounds the response until paginated CRUD lands.
     */
    private static final int MAX_TEMPLATES = 200;

    private final PromptTemplateRepository promptTemplateRepository;

    @Transactional(readOnly = true)
    public List<PromptTemplateResponse> listTemplates(AuthenticatedEmployee actor) {
        requireAdmin(actor);
        return promptTemplateRepository
                .findAllByOrderByNameAscLocaleAscIdDesc(PageRequest.of(0, MAX_TEMPLATES)).stream()
                .map(PromptTemplateResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PromptTemplateResponse getTemplate(AuthenticatedEmployee actor, Long id) {
        requireAdmin(actor);
        return promptTemplateRepository.findById(id)
                .map(PromptTemplateResponse::from)
                .orElseThrow(() -> new ResourceNotFoundApplicationException(
                        "Prompt template not found: " + id));
    }

    private void requireAdmin(AuthenticatedEmployee actor) {
        if (actor.role() != UserRole.ADMIN) {
            throw new ForbiddenApplicationException("Only admins can view prompt templates");
        }
    }
}
