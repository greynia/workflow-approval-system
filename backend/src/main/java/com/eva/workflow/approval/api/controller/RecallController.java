package com.eva.workflow.approval.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eva.workflow.approval.api.dto.recall.PendingRecallCountResponse;
import com.eva.workflow.approval.api.dto.recall.PendingRecallResponse;
import com.eva.workflow.approval.api.dto.recall.RecallDecisionRequest;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.application.recall.RecallApplicationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/recalls")
@Validated
@RequiredArgsConstructor
public class RecallController {

    private final RecallApplicationService recallApplicationService;

    @GetMapping("/pending")
    public ResponseEntity<List<PendingRecallResponse>> pending(Authentication authentication) {
        AuthenticatedEmployee manager = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(recallApplicationService.getPendingRecalls(manager));
    }

    @GetMapping("/pending/count")
    public ResponseEntity<PendingRecallCountResponse> pendingCount(Authentication authentication) {
        AuthenticatedEmployee manager = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(recallApplicationService.getPendingRecallCount(manager));
    }

    @PostMapping("/{stepId}/approve")
    public ResponseEntity<Void> approve(
            @PathVariable Long stepId,
            Authentication authentication
    ) {
        AuthenticatedEmployee manager = (AuthenticatedEmployee) authentication.getPrincipal();
        recallApplicationService.approveRecall(stepId, manager);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{stepId}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable Long stepId,
            @Valid @RequestBody(required = false) RecallDecisionRequest request,
            Authentication authentication
    ) {
        AuthenticatedEmployee manager = (AuthenticatedEmployee) authentication.getPrincipal();
        String comment = (request != null) ? request.comment() : null;
        recallApplicationService.rejectRecall(stepId, manager, comment);
        return ResponseEntity.noContent().build();
    }
}
