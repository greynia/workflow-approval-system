package com.eva.workflow.approval.api.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eva.workflow.approval.api.dto.common.EmployeeSummaryResponse;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.application.employee.EmployeeQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeQueryService employeeQueryService;

    @GetMapping
    public ResponseEntity<List<EmployeeSummaryResponse>> list(
            Authentication authentication,
            @RequestParam(required = false) String query
    ) {
        AuthenticatedEmployee authenticatedEmployee = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(employeeQueryService.getSelectableEmployees(authenticatedEmployee, query));
    }

    @GetMapping("/available-deputies")
    public ResponseEntity<List<EmployeeSummaryResponse>> availableDeputies(
            Authentication authentication,
            @RequestParam(required = false) String query,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        AuthenticatedEmployee authenticatedEmployee = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(employeeQueryService.getAvailableDeputies(authenticatedEmployee, query, startTime, endTime));
    }
}
