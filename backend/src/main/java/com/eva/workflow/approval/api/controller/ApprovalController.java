package com.eva.workflow.approval.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eva.workflow.approval.api.dto.approval.PendingApprovalCountResponse;
import com.eva.workflow.approval.api.dto.approval.PendingApprovalResponse;
import com.eva.workflow.approval.application.approval.ApprovalQueryService;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {

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
}
