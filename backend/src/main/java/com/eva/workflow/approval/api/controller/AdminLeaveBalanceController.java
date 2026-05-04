package com.eva.workflow.approval.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eva.workflow.approval.api.dto.admin.AdminLeaveBalanceResponse;
import com.eva.workflow.approval.api.dto.admin.AdjustLeaveBalanceRequest;
import com.eva.workflow.approval.api.dto.admin.InitYearBalancesRequest;
import com.eva.workflow.approval.api.dto.admin.YearInitResult;
import com.eva.workflow.approval.api.dto.common.PageResponse;
import com.eva.workflow.approval.application.admin.AdminLeaveBalanceService;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/admin/leave-balances")
@RequiredArgsConstructor
public class AdminLeaveBalanceController {

    private final AdminLeaveBalanceService adminLeaveBalanceService;

    @GetMapping
    public ResponseEntity<PageResponse<AdminLeaveBalanceResponse>> getBalances(
            @RequestParam @NotNull Integer year,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Authentication authentication
    ) {
        AuthenticatedEmployee actor = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(adminLeaveBalanceService.getBalances(actor, year, employeeId, page, size));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AdminLeaveBalanceResponse> adjustBalance(
            @PathVariable Long id,
            @Valid @RequestBody AdjustLeaveBalanceRequest request,
            Authentication authentication
    ) {
        AuthenticatedEmployee actor = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(adminLeaveBalanceService.adjustBalance(actor, id, request));
    }

    @PostMapping("/year-init")
    public ResponseEntity<YearInitResult> initYearBalances(
            @Valid @RequestBody InitYearBalancesRequest request,
            Authentication authentication
    ) {
        AuthenticatedEmployee actor = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(adminLeaveBalanceService.initYearBalances(actor, request));
    }
}
