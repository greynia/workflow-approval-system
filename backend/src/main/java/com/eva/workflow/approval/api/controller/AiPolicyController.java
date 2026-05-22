package com.eva.workflow.approval.api.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eva.workflow.approval.api.dto.aireview.PolicySearchResponse;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.application.policy.PolicySearchService;

import lombok.RequiredArgsConstructor;

/**
 * AI-facing policy search. Intended for the enterprise-ai-skill-harness PolicySearchSkill.
 * Authorization (AI_AGENT or ADMIN) is enforced in the service, consistent with the other
 * {@code /api/ai/**} endpoints.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.ai.gemini", name = "api-key")
public class AiPolicyController {

    private final PolicySearchService policySearchService;

    @GetMapping("/policy-search")
    public ResponseEntity<PolicySearchResponse> search(
            @RequestParam("query") String query,
            @RequestParam(value = "topK", required = false) Integer topK,
            @RequestParam(value = "locale", required = false) String locale,
            Authentication authentication
    ) {
        AuthenticatedEmployee actor = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(policySearchService.search(actor, query, topK, locale));
    }
}
