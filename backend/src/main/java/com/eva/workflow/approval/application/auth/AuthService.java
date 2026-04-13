package com.eva.workflow.approval.application.auth;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.api.dto.auth.EmployeeResponse;
import com.eva.workflow.approval.api.exception.InvalidCredentialsException;
import com.eva.workflow.approval.api.dto.auth.LoginRequest;
import com.eva.workflow.approval.api.exception.ResourceNotFoundException;
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

    @Transactional(readOnly = true)
    public AuthResult login(LoginRequest request) {
        EmployeeEntity employee = employeeRepository.findByEmail(request.email())
                .filter(EmployeeEntity::getActive)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), employee.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtProvider.generateToken(employee);
        return new AuthResult(token, employee.getId(), employee.getName(), employee.getRole());
    }

    @Cacheable(value = CacheNames.CURRENT_EMPLOYEE, key = "#authenticatedEmployee.employeeId()")
    @Transactional(readOnly = true)
    public EmployeeResponse getCurrentEmployee(AuthenticatedEmployee authenticatedEmployee) {
        EmployeeEntity employee = employeeRepository.findById(authenticatedEmployee.employeeId())
                .filter(EmployeeEntity::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("Current employee not found"));

        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeNo(),
                employee.getName(),
                employee.getEmail(),
                employee.getRole(),
                employee.getDepartment().getId(),
                employee.getManager() != null ? employee.getManager().getId() : null
        );
    }
}
