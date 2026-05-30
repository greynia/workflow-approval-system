package com.eva.workflow.approval.application.policy;

import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.api.dto.aireview.PolicySearchResponse;
import com.eva.workflow.approval.api.dto.aireview.PolicySearchResponse.Match;
import com.eva.workflow.approval.application.audit.AuditLogService;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.application.exception.BadRequestApplicationException;
import com.eva.workflow.approval.application.exception.ForbiddenApplicationException;
import com.eva.workflow.approval.application.exception.ResourceNotFoundApplicationException;
import com.eva.workflow.approval.common.enums.UserRole;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;

/**
 * AI-facing semantic search over company policy. Enforces the AI_AGENT/ADMIN guard and audits the
 * call (mirroring {@code AiContextQueryService}), then delegates the actual two-stage retrieval to
 * {@link PolicyRetrievalService} and maps the result to the API DTO.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.ai.gemini", name = "api-key")
public class PolicySearchService {

    private final PolicyRetrievalService policyRetrievalService;
    private final AuditLogService auditLogService;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public PolicySearchResponse search(
            AuthenticatedEmployee actor, String query, Integer topKParam, String locale) {
        requireAiAgentOrAdmin(actor);
        if (query == null || query.isBlank()) {
            throw new BadRequestApplicationException("'query' must not be blank");
        }

        PolicyRetrieval retrieval = policyRetrievalService.retrieve(query, locale, topKParam);

        List<Match> matches = retrieval.matches().stream()
                .map(m -> new Match(m.section(), m.source(), m.chunkIndex(), m.content(), m.score()))
                .toList();

        // Policy search is not tied to a domain entity; audit it against the actor
        // performing the search (entity_id is NOT NULL in the shared audit schema).
        auditLogService.log(
                "AI_POLICY_SEARCH", actor.employeeId(), "READ", resolveActorEntity(actor),
                Map.of("candidateCount", retrieval.candidateCount(),
                        "matchCount", matches.size(),
                        "reranked", retrieval.reranked()));

        return new PolicySearchResponse(query, retrieval.finalTopK(), retrieval.reranked(), matches);
    }

    private void requireAiAgentOrAdmin(AuthenticatedEmployee actor) {
        UserRole role = actor.role();
        if (role != UserRole.AI_AGENT && role != UserRole.ADMIN) {
            throw new ForbiddenApplicationException("Only AI agents or admins can search policy");
        }
    }

    private EmployeeEntity resolveActorEntity(AuthenticatedEmployee actor) {
        return employeeRepository.findById(actor.employeeId())
                .orElseThrow(() -> new ResourceNotFoundApplicationException(
                        "Actor employee not found: " + actor.employeeId()));
    }
}
