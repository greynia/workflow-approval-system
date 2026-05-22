package com.eva.workflow.approval.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eva.workflow.approval.api.dto.admin.PromptTemplateResponse;
import com.eva.workflow.approval.application.admin.PromptQueryService;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/prompts")
@RequiredArgsConstructor
public class AdminPromptController {

    private final PromptQueryService promptQueryService;

    @GetMapping
    public ResponseEntity<List<PromptTemplateResponse>> list(Authentication authentication) {
        AuthenticatedEmployee actor = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(promptQueryService.listTemplates(actor));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromptTemplateResponse> get(
            @PathVariable Long id, Authentication authentication) {
        AuthenticatedEmployee actor = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(promptQueryService.getTemplate(actor, id));
    }
}
