package com.eva.workflow.approval.application.approval;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.application.exception.ApplicationConfigurationException;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ManagerChainResolver {

    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public List<Long> resolveEligibleManagerChainIds(EmployeeEntity applicant) {
        List<EmployeeRepository.ManagerChainRow> managerChainRows =
                employeeRepository.findManagerChainRows(applicant.getId());

        boolean cycleDetected = managerChainRows.stream()
                .anyMatch(row -> Boolean.TRUE.equals(row.getCycle()));
        if (cycleDetected) {
            throw new ApplicationConfigurationException(
                    "Manager cycle detected in hierarchy for employee: " + applicant.getId());
        }

        return managerChainRows.stream()
                .filter(row -> Boolean.TRUE.equals(row.getActive()))
                .map(EmployeeRepository.ManagerChainRow::getEmployeeId)
                .toList();
    }
}
