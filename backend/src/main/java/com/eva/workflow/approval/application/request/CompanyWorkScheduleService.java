package com.eva.workflow.approval.application.request;

import java.time.LocalDate;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.application.exception.ResourceNotFoundApplicationException;
import com.eva.workflow.approval.infrastructure.cache.CacheNames;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.CompanyWorkScheduleEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.CompanyWorkScheduleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompanyWorkScheduleService {

    private final CompanyWorkScheduleRepository companyWorkScheduleRepository;

    @Cacheable(value = CacheNames.COMPANY_WORK_SCHEDULE, key = "#date")
    @Transactional(readOnly = true)
    public CompanyWorkScheduleEntity getEffectiveSchedule(LocalDate date) {
        return companyWorkScheduleRepository.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(date)
                .orElseThrow(() -> new ResourceNotFoundApplicationException("Company work schedule not found"));
    }
}
