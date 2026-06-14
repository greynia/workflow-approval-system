package com.eva.workflow.approval.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eva.workflow.approval.api.dto.admin.AiStatsResponse;
import com.eva.workflow.approval.api.dto.admin.RecentFailuresResponse;
import com.eva.workflow.approval.application.admin.AiStatsQueryService;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AdminAiStatsController {

    private final AiStatsQueryService aiStatsQueryService;

    @GetMapping("/stats")
    public ResponseEntity<AiStatsResponse> getStats(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            Authentication authentication
    ) {
        AuthenticatedEmployee actor = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(aiStatsQueryService.getStats(actor, from, to));
    }

    @GetMapping("/recent-failures")
    public ResponseEntity<RecentFailuresResponse> getRecentFailures(
            @RequestParam(required = false) Integer limit,
            Authentication authentication
    ) {
        AuthenticatedEmployee actor = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(aiStatsQueryService.getRecentFailures(actor, limit));
    }
}
