package com.eva.workflow.approval.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eva.workflow.approval.api.dto.approval.ApprovalDecisionRequest;
import com.eva.workflow.approval.api.dto.approval.PendingApprovalCountResponse;
import com.eva.workflow.approval.api.dto.approval.PendingApprovalResponse;
import com.eva.workflow.approval.application.approval.ApprovalApplicationService;
import com.eva.workflow.approval.application.approval.ApprovalQueryService;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;

import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalApplicationService approvalApplicationService;
    private final ApprovalQueryService approvalQueryService;

    @GetMapping("/pending")
    public ResponseEntity<List<PendingApprovalResponse>> pending(Authentication authentication) {
        AuthenticatedEmployee authenticatedEmployee = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(approvalQueryService.getPendingApprovals(authenticatedEmployee));
    }

    @GetMapping("/pending/count")
    public ResponseEntity<PendingApprovalCountResponse> pendingCount(Authentication authentication) {
        AuthenticatedEmployee authenticatedEmployee = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(approvalQueryService.getPendingApprovalCount(authenticatedEmployee));
    }

    @PostMapping("/{stepId}/approve")
    public ResponseEntity<Void> approve(
            @PathVariable Long stepId,
            @Valid @RequestBody(required = false) ApprovalDecisionRequest request,
            Authentication authentication
    ) {
        AuthenticatedEmployee authenticatedEmployee = (AuthenticatedEmployee) authentication.getPrincipal();
        approvalApplicationService.approveStep(stepId, authenticatedEmployee, normalizeRequest(request));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/{stepId}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable Long stepId,
            @Valid @RequestBody(required = false) ApprovalDecisionRequest request,
            Authentication authentication
    ) {
        AuthenticatedEmployee authenticatedEmployee = (AuthenticatedEmployee) authentication.getPrincipal();
        approvalApplicationService.rejectStep(stepId, authenticatedEmployee, normalizeRequest(request));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private ApprovalDecisionRequest normalizeRequest(ApprovalDecisionRequest request) {
        if (request == null) {
            return new ApprovalDecisionRequest(null);
        }
        return request;
    }
}
