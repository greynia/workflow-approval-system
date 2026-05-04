package com.eva.workflow.approval.application.admin;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.api.dto.admin.CompanyWorkScheduleResponse;
import com.eva.workflow.approval.api.dto.admin.EmployeeScheduleResponse;
import com.eva.workflow.approval.api.dto.common.PageResponse;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.application.exception.ForbiddenApplicationException;
import com.eva.workflow.approval.application.request.CompanyWorkScheduleService;
import com.eva.workflow.approval.common.enums.UserRole;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.CompanyWorkScheduleEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeScheduleEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.EmployeeScheduleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminScheduleService {

    private final CompanyWorkScheduleService companyWorkScheduleService;
    private final EmployeeScheduleRepository employeeScheduleRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public CompanyWorkScheduleResponse getCompanyWorkSchedule(AuthenticatedEmployee actor) {
        requireAdmin(actor);
        CompanyWorkScheduleEntity entity = companyWorkScheduleService.getEffectiveSchedule(LocalDate.now(clock));
        return new CompanyWorkScheduleResponse(
                entity.getId(),
                entity.getWorkStart(),
                entity.getWorkEnd(),
                entity.getLunchStart(),
                entity.getLunchEnd(),
                entity.getWorkDays(),
                entity.getEffectiveFrom()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeScheduleResponse> getEmployeeSchedules(AuthenticatedEmployee actor, int page, int size) {
        requireAdmin(actor);
        Page<EmployeeScheduleEntity> result = employeeScheduleRepository.findAllWithEmployee(PageRequest.of(page, size));
        return new PageResponse<>(
                result.getContent().stream()
                        .map(this::toResponse)
                        .toList(),
                result.getNumber(),
                result.getTotalElements(),
                result.getSize(),
                result.getTotalPages()
        );
    }

    private EmployeeScheduleResponse toResponse(EmployeeScheduleEntity entity) {
        return new EmployeeScheduleResponse(
                entity.getId(),
                entity.getEmployee().getId(),
                entity.getEmployee().getName(),
                entity.getEmployee().getEmployeeNo(),
                entity.getScheduleType(),
                entity.getEffectiveFrom(),
                entity.getEffectiveTo()
        );
    }

    private void requireAdmin(AuthenticatedEmployee actor) {
        if (actor.role() != UserRole.ADMIN) {
            throw new ForbiddenApplicationException("Only admins can view schedule settings");
        }
    }
}
