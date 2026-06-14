package com.eva.workflow.approval.application.request;

import java.time.LocalDate;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.infrastructure.cache.CacheNames;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeScheduleEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.EmployeeScheduleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeScheduleService {

    private final EmployeeScheduleRepository employeeScheduleRepository;

    @Cacheable(value = CacheNames.EMPLOYEE_SCHEDULE, key = "#employeeId + ':' + #date")
    @Transactional(readOnly = true)
    public EmployeeScheduleEntity getActiveSchedule(Long employeeId, LocalDate date) {
        return employeeScheduleRepository.findActiveSchedules(employeeId, date).stream()
                .findFirst()
                .orElse(null);
    }
}
