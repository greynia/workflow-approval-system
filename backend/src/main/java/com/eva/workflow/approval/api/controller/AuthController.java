package com.eva.workflow.approval.api.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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
import com.eva.workflow.approval.application.auth.AuthResult;
import com.eva.workflow.approval.application.auth.AuthService;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.infrastructure.security.JwtProperties;
import com.eva.workflow.approval.infrastructure.security.SecurityConstants;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authService.login(request);

        ResponseCookie cookie = ResponseCookie.from(SecurityConstants.TOKEN_COOKIE_NAME, result.token())
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(jwtProperties.expirationSeconds())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new LoginResponse(result.employeeId(), result.name(), result.role(), result.permissions()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = ResponseCookie.from(SecurityConstants.TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<EmployeeResponse> me(Authentication authentication) {
        AuthenticatedEmployee authenticatedEmployee = (AuthenticatedEmployee) authentication.getPrincipal();
        return ResponseEntity.ok(authService.getCurrentEmployee(authenticatedEmployee));
    }
}
