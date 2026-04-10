package com.eva.workflow.approval.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eva.workflow.approval.api.dto.auth.EmployeeResponse;
import com.eva.workflow.approval.api.dto.auth.LoginRequest;
import com.eva.workflow.approval.api.dto.auth.LoginResponse;
import com.eva.workflow.approval.application.auth.AuthService;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<EmployeeResponse> me(Authentication authentication) {
        AuthenticatedEmployee authenticatedEmployee = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(authService.getCurrentEmployee(authenticatedEmployee));
    }
}
