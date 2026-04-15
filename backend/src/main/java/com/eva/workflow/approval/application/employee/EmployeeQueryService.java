package com.eva.workflow.approval.application.employee;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.api.dto.common.EmployeeSummaryResponse;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.application.exception.BadRequestApplicationException;
import com.eva.workflow.approval.common.enums.RequestStatus;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeQueryService {

    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public List<EmployeeSummaryResponse> getSelectableEmployees(AuthenticatedEmployee authenticatedEmployee) {
        return employeeRepository.findByActiveTrueAndIdNotOrderByNameAsc(authenticatedEmployee.employeeId()).stream()
                .map(employee -> new EmployeeSummaryResponse(employee.getId(), employee.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeSummaryResponse> getAvailableDeputies(
            AuthenticatedEmployee authenticatedEmployee,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestApplicationException("endTime must be after startTime");
        }

        return employeeRepository.findAvailableDeputies(
                        authenticatedEmployee.employeeId(),
                        startTime,
                        endTime,
                        List.of(RequestStatus.APPROVED, RequestStatus.PENDING)
                ).stream()
                .map(employee -> new EmployeeSummaryResponse(employee.getId(), employee.getName()))
                .toList();
    }
}
