package com.eva.workflow.approval.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.eva.workflow.approval.api.dto.aireview.AiReviewResponse;
import com.eva.workflow.approval.api.dto.common.PageResponse;
import com.eva.workflow.approval.api.dto.leave.CreateLeaveRequest;
import com.eva.workflow.approval.api.dto.leave.LeaveBalanceResponse;
import com.eva.workflow.approval.api.dto.leave.PendingRequestCountResponse;
import com.eva.workflow.approval.api.dto.leave.LeaveCalculationRequest;
import com.eva.workflow.approval.api.dto.leave.LeaveCalculationResponse;
import com.eva.workflow.approval.api.dto.leave.LeaveRequestDetailResponse;
import com.eva.workflow.approval.api.dto.leave.LeaveRequestSummaryResponse;
import com.eva.workflow.approval.application.aireview.AiReviewQueryService;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.application.request.LeaveRequestApplicationService;

import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/requests")
@Validated
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestApplicationService leaveRequestApplicationService;
    private final AiReviewQueryService aiReviewQueryService;

    @PostMapping
    public ResponseEntity<LeaveRequestDetailResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateLeaveRequest request
    ) {
        AuthenticatedEmployee authenticatedEmployee = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leaveRequestApplicationService.createRequest(authenticatedEmployee, request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<LeaveRequestSummaryResponse>> list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        AuthenticatedEmployee authenticatedEmployee = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(leaveRequestApplicationService.getRequests(authenticatedEmployee, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveRequestDetailResponse> detail(
            Authentication authentication,
            @PathVariable Long id
    ) {
        AuthenticatedEmployee authenticatedEmployee = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(leaveRequestApplicationService.getRequestDetail(authenticatedEmployee, id));
    }

    @PostMapping("/calculate")
    public ResponseEntity<LeaveCalculationResponse> calculate(
            Authentication authentication,
            @Valid @RequestBody LeaveCalculationRequest request
    ) {
        AuthenticatedEmployee authenticatedEmployee = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(leaveRequestApplicationService.calculateDuration(authenticatedEmployee, request));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(Authentication authentication, @PathVariable Long id) {
        AuthenticatedEmployee authenticatedEmployee = (AuthenticatedEmployee) authentication.getPrincipal();
        leaveRequestApplicationService.cancelRequest(authenticatedEmployee, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/ai-review")
    public ResponseEntity<AiReviewResponse> getAiReview(
            Authentication authentication,
            @PathVariable Long id
    ) {
        AuthenticatedEmployee authenticatedEmployee = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(aiReviewQueryService.getAiReview(authenticatedEmployee, id));
    }

    @GetMapping("/pending/count")
    public ResponseEntity<PendingRequestCountResponse> pendingCount(Authentication authentication) {
        AuthenticatedEmployee authenticatedEmployee = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(leaveRequestApplicationService.getPendingRequestCount(authenticatedEmployee));
    }

    @GetMapping("/balance")
    public ResponseEntity<List<LeaveBalanceResponse>> balance(
            Authentication authentication,
            @RequestParam(defaultValue = "0") @Min(0) int year
    ) {
        AuthenticatedEmployee authenticatedEmployee = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(leaveRequestApplicationService.getLeaveBalances(authenticatedEmployee, year));
    }
}
