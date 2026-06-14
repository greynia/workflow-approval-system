package com.eva.workflow.approval.application.admin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.api.dto.admin.AdminLeaveBalanceResponse;
import com.eva.workflow.approval.api.dto.admin.AdjustLeaveBalanceRequest;
import com.eva.workflow.approval.api.dto.admin.InitYearBalancesRequest;
import com.eva.workflow.approval.api.dto.admin.YearInitResult;
import com.eva.workflow.approval.api.dto.common.PageResponse;
import com.eva.workflow.approval.application.audit.AuditLogService;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.application.exception.BadRequestApplicationException;
import com.eva.workflow.approval.application.exception.ForbiddenApplicationException;
import com.eva.workflow.approval.application.exception.ResourceNotFoundApplicationException;
import com.eva.workflow.approval.application.request.LeaveBalanceService;
import com.eva.workflow.approval.common.AuditEntityTypes;
import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.common.enums.UserRole;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.LeaveBalanceEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.EmployeeRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.LeaveBalanceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminLeaveBalanceService {

    private static final LeaveType[] TRACKABLE_TYPES = { LeaveType.ANNUAL, LeaveType.SICK, LeaveType.PERSONAL };

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceService leaveBalanceService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public PageResponse<AdminLeaveBalanceResponse> getBalances(
            AuthenticatedEmployee actor, int year, Long employeeId, int page, int size) {
        requireAdmin(actor);

        Page<LeaveBalanceEntity> balancePage;
        if (employeeId != null) {
            balancePage = leaveBalanceRepository.findAllByEmployeeIdAndYearOrderByLeaveType(
                    employeeId, year, PageRequest.of(page, size));
        } else {
            balancePage = leaveBalanceRepository.findAllByYearOrderByEmployeeIdAndLeaveType(
                    year, PageRequest.of(page, size));
        }

        Set<Long> empIds = balancePage.stream()
                .map(LeaveBalanceEntity::getEmployeeId)
                .collect(Collectors.toSet());

        Map<Long, String> nameMap = employeeRepository.findAllById(empIds).stream()
                .collect(Collectors.toMap(EmployeeEntity::getId, EmployeeEntity::getName));

        List<AdminLeaveBalanceResponse> items = balancePage.stream()
                .map(b -> toResponse(b, nameMap.getOrDefault(b.getEmployeeId(), "Unknown")))
                .toList();

        return new PageResponse<>(
                items,
                balancePage.getNumber(),
                balancePage.getTotalElements(),
                balancePage.getSize(),
                balancePage.getTotalPages()
        );
    }

    @Transactional
    public AdminLeaveBalanceResponse adjustBalance(
            AuthenticatedEmployee actor, Long id, AdjustLeaveBalanceRequest request) {
        requireAdmin(actor);

        LeaveBalanceEntity balance = leaveBalanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundApplicationException("Leave balance not found"));

        if (request.quotaMinutes() < balance.getUsedMinutes()) {
            throw new BadRequestApplicationException("QUOTA_BELOW_USED_MINUTES");
        }

        int oldQuota = balance.getQuotaMinutes();
        balance.updateQuotaMinutes(request.quotaMinutes());

        auditLogService.log(
                AuditEntityTypes.LEAVE_BALANCE,
                id,
                "ADJUST",
                employeeRepository.getReferenceById(actor.employeeId()),
                buildAdjustDetail(oldQuota, request.quotaMinutes(), request.reason())
        );

        String employeeName = employeeRepository.findById(balance.getEmployeeId())
                .map(EmployeeEntity::getName)
                .orElse("Unknown");

        return toResponse(balance, employeeName);
    }

    @Transactional
    public YearInitResult initYearBalances(AuthenticatedEmployee actor, InitYearBalancesRequest request) {
        requireAdmin(actor);

        List<EmployeeEntity> employees = employeeRepository.findAll();
        int initialized = 0;
        int skipped = 0;

        for (EmployeeEntity employee : employees) {
            for (LeaveType leaveType : TRACKABLE_TYPES) {
                int quotaMinutes = leaveBalanceService.calculateQuotaMinutes(
                        leaveType, employee.getHireDate(), request.year());
                int inserted = leaveBalanceRepository.insertIfAbsent(
                        employee.getId(), request.year(), leaveType.name(), quotaMinutes);
                if (inserted > 0) {
                    initialized++;
                } else {
                    skipped++;
                }
            }
        }

        return new YearInitResult(initialized, skipped);
    }

    private AdminLeaveBalanceResponse toResponse(LeaveBalanceEntity b, String employeeName) {
        return new AdminLeaveBalanceResponse(
                b.getId(),
                b.getEmployeeId(),
                employeeName,
                b.getLeaveType().name(),
                b.getQuotaMinutes(),
                b.getUsedMinutes(),
                b.getRemainingMinutes(),
                b.getUpdatedAt()
        );
    }

    private Map<String, Object> buildAdjustDetail(int oldQuota, int newQuota, String reason) {
        return Map.of(
                "oldQuotaMinutes", oldQuota,
                "newQuotaMinutes", newQuota,
                "reason", reason
        );
    }

    private void requireAdmin(AuthenticatedEmployee actor) {
        if (actor.role() != UserRole.ADMIN) {
            throw new ForbiddenApplicationException("Only admins can manage leave balances");
        }
    }
}
