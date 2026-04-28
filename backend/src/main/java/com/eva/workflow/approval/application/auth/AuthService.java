package com.eva.workflow.approval.application.auth;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.api.dto.auth.EmployeeResponse;
import com.eva.workflow.approval.api.dto.auth.LoginRequest;
import com.eva.workflow.approval.common.RolePermissions;
import com.eva.workflow.approval.application.exception.InvalidCredentialsApplicationException;
import com.eva.workflow.approval.application.exception.ResourceNotFoundApplicationException;
import com.eva.workflow.approval.infrastructure.cache.CacheNames;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.EmployeeRepository;
import com.eva.workflow.approval.infrastructure.security.JwtProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResult login(LoginRequest request) {
        EmployeeEntity employee = employeeRepository.findByEmail(request.email())
                .filter(EmployeeEntity::getActive)
                .orElseThrow(() -> new InvalidCredentialsApplicationException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), employee.getPasswordHash())) {
            throw new InvalidCredentialsApplicationException("Invalid email or password");
        }

        String accessToken = jwtProvider.generateToken(employee);
        IssuedRefreshToken refreshToken = refreshTokenService.issue(employee);
        return new AuthResult(
                accessToken,
                refreshToken.token(),
                employee.getId(),
                employee.getName(),
                employee.getRole(),
                RolePermissions.of(employee.getRole())
        );
    }

    @Transactional
    public AuthResult refresh(String rawRefreshToken) {
        IssuedRefreshToken refreshToken = refreshTokenService.rotate(rawRefreshToken);
        EmployeeEntity employee = refreshToken.employee();
        String accessToken = jwtProvider.generateToken(employee);
        return new AuthResult(
                accessToken,
                refreshToken.token(),
                employee.getId(),
                employee.getName(),
                employee.getRole(),
                RolePermissions.of(employee.getRole())
        );
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    @Cacheable(value = CacheNames.CURRENT_EMPLOYEE, key = "#authenticatedEmployee.employeeId()")
    @Transactional(readOnly = true)
    public EmployeeResponse getCurrentEmployee(AuthenticatedEmployee authenticatedEmployee) {
        EmployeeEntity employee = employeeRepository.findById(authenticatedEmployee.employeeId())
                .filter(EmployeeEntity::getActive)
                .orElseThrow(() -> new ResourceNotFoundApplicationException("Current employee not found"));

        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeNo(),
                employee.getName(),
                employee.getEmail(),
                employee.getRole(),
                employee.getDepartment().getId(),
                employee.getManager() != null ? employee.getManager().getId() : null,
                RolePermissions.of(employee.getRole())
        );
    }
}
