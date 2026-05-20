package com.eva.workflow.approval.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eva.workflow.approval.api.dto.aireview.AiContextResponse;
import com.eva.workflow.approval.api.dto.aireview.RuleEvaluationResponse;
import com.eva.workflow.approval.application.aireview.AiContextQueryService;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai/requests")
@RequiredArgsConstructor
public class AiContextController {

    private final AiContextQueryService aiContextQueryService;

    @GetMapping("/{id}/context")
    public ResponseEntity<AiContextResponse> getContext(
            @PathVariable("id") Long requestId,
            Authentication authentication
    ) {
        AuthenticatedEmployee actor = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(aiContextQueryService.getContext(actor, requestId));
    }

    @GetMapping("/{id}/rule-evaluation")
    public ResponseEntity<RuleEvaluationResponse> getRuleEvaluation(
            @PathVariable("id") Long requestId,
            @RequestParam(value = "locale", required = false, defaultValue = "en") String locale,
            Authentication authentication
    ) {
        AuthenticatedEmployee actor = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(aiContextQueryService.evaluateRules(actor, requestId, locale));
    }
}
