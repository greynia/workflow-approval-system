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
import com.eva.workflow.approval.domain.policy.service.EmbeddingPort;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.EmployeeRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.PolicyChunkMatchRow;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.PolicyChunkRepository;

import lombok.RequiredArgsConstructor;

/**
 * AI-facing semantic search over company policy. Embeds the query with the same model
 * used at ingestion, runs a pgvector cosine search, and returns the top matches.
 * Mirrors the authorization + audit pattern of {@code AiContextQueryService}.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.ai.gemini", name = "api-key")
public class PolicySearchService {

    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 20;

    private final EmbeddingPort embeddingPort;
    private final PolicyChunkRepository policyChunkRepository;
    private final AuditLogService auditLogService;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public PolicySearchResponse search(
            AuthenticatedEmployee actor, String query, Integer topKParam, String locale) {
        requireAiAgentOrAdmin(actor);
        if (query == null || query.isBlank()) {
            throw new BadRequestApplicationException("'query' must not be blank");
        }
        int topK = clampTopK(topKParam);

        float[] queryVector = embeddingPort.embed(query);
        List<PolicyChunkMatchRow> rows =
                policyChunkRepository.searchSimilar(toVectorLiteral(queryVector), locale, topK);

        List<Match> matches = rows.stream()
                .map(row -> new Match(row.getSection(), row.getContent(), row.getScore()))
                .toList();

        // Policy search is not tied to a domain entity; audit it against the actor
        // performing the search (entity_id is NOT NULL in the shared audit schema).
        auditLogService.log(
                "AI_POLICY_SEARCH", actor.employeeId(), "READ", resolveActorEntity(actor),
                Map.of("topK", topK, "matchCount", matches.size()));

        return new PolicySearchResponse(query, topK, matches);
    }

    private int clampTopK(Integer topK) {
        if (topK == null) {
            return DEFAULT_TOP_K;
        }
        if (topK < 1) {
            return 1;
        }
        return Math.min(topK, MAX_TOP_K);
    }

    /** Formats a vector as a pgvector literal: {@code [0.1,0.2,...]}. */
    private String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 8);
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
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
