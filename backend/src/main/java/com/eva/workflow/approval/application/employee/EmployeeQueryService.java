package com.eva.workflow.approval.application.employee;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.api.dto.common.EmployeeSummaryResponse;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
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
}
