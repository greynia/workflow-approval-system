package com.eva.workflow.approval.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eva.workflow.approval.api.dto.common.PageResponse;
import com.eva.workflow.approval.api.dto.leave.CreateLeaveRequest;
import com.eva.workflow.approval.api.dto.leave.LeaveRequestDetailResponse;
import com.eva.workflow.approval.api.dto.leave.LeaveRequestSummaryResponse;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.application.request.LeaveRequestApplicationService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/requests")
@Validated
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestApplicationService leaveRequestApplicationService;

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
            @RequestParam(defaultValue = "10") @Min(1) int size
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
}
