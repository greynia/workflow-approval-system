package com.eva.workflow.approval.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eva.workflow.approval.api.dto.admin.CompanyWorkScheduleResponse;
import com.eva.workflow.approval.api.dto.admin.EmployeeScheduleResponse;
import com.eva.workflow.approval.api.dto.common.PageResponse;
import com.eva.workflow.approval.application.admin.AdminScheduleService;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminScheduleController {

    private final AdminScheduleService adminScheduleService;

    @GetMapping("/company-work-schedules")
    public ResponseEntity<CompanyWorkScheduleResponse> getCompanyWorkSchedule(Authentication authentication) {
        AuthenticatedEmployee actor = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(adminScheduleService.getCompanyWorkSchedule(actor));
    }

    @GetMapping("/employee-schedules")
    public ResponseEntity<PageResponse<EmployeeScheduleResponse>> getEmployeeSchedules(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Authentication authentication
    ) {
        AuthenticatedEmployee actor = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(adminScheduleService.getEmployeeSchedules(actor, page, size));
    }
}
