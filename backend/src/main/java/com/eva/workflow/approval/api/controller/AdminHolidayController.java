package com.eva.workflow.approval.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eva.workflow.approval.api.dto.admin.CreateHolidayRequest;
import com.eva.workflow.approval.api.dto.admin.HolidayImportResultResponse;
import com.eva.workflow.approval.api.dto.admin.HolidayResponse;
import com.eva.workflow.approval.application.admin.AdminHolidayService;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/admin/holidays")
@RequiredArgsConstructor
public class AdminHolidayController {

    private final AdminHolidayService adminHolidayService;

    @GetMapping
    public ResponseEntity<List<HolidayResponse>> getHolidays(
            @RequestParam(defaultValue = "2026") @Min(2020) @Max(2099) int year,
            Authentication authentication
    ) {
        AuthenticatedEmployee actor = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(adminHolidayService.getHolidaysByYear(actor, year));
    }

    @PostMapping
    public ResponseEntity<HolidayResponse> addHoliday(
            @Valid @RequestBody CreateHolidayRequest request,
            Authentication authentication
    ) {
        AuthenticatedEmployee actor = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(adminHolidayService.addHoliday(actor, request));
    }

    @PostMapping("/import")
    public ResponseEntity<HolidayImportResultResponse> importHolidays(
            @RequestParam(defaultValue = "2026") @Min(2020) @Max(2099) int year,
            Authentication authentication
    ) {
        AuthenticatedEmployee actor = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(adminHolidayService.importFromCalendar(actor, year));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHoliday(
            @PathVariable Long id,
            Authentication authentication
    ) {
        AuthenticatedEmployee actor = (AuthenticatedEmployee) authentication.getPrincipal();
        adminHolidayService.deleteHoliday(actor, id);
        return ResponseEntity.noContent().build();
    }
}
