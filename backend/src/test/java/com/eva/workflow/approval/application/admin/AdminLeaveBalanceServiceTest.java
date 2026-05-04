package com.eva.workflow.approval.application.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.eva.workflow.approval.api.dto.admin.AdjustLeaveBalanceRequest;
import com.eva.workflow.approval.api.dto.admin.AdminLeaveBalanceResponse;
import com.eva.workflow.approval.api.dto.admin.InitYearBalancesRequest;
import com.eva.workflow.approval.api.dto.admin.YearInitResult;
import com.eva.workflow.approval.api.dto.common.PageResponse;
import com.eva.workflow.approval.application.audit.AuditLogService;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.application.exception.BadRequestApplicationException;
import com.eva.workflow.approval.application.exception.ForbiddenApplicationException;
import com.eva.workflow.approval.application.request.LeaveBalanceService;
import com.eva.workflow.approval.common.AuditEntityTypes;
import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.common.enums.UserRole;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.LeaveBalanceEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.EmployeeRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.LeaveBalanceRepository;

@ExtendWith(MockitoExtension.class)
class AdminLeaveBalanceServiceTest {

    @Mock private LeaveBalanceRepository leaveBalanceRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private LeaveBalanceService leaveBalanceService;
    @Mock private AuditLogService auditLogService;

    private AdminLeaveBalanceService service;

    private final AuthenticatedEmployee admin =
            new AuthenticatedEmployee(1L, "admin@example.com", "Admin", UserRole.ADMIN, List.of());
    private final AuthenticatedEmployee employee =
            new AuthenticatedEmployee(7L, "user@example.com", "User", UserRole.EMPLOYEE, List.of());

    @BeforeEach
    void setUp() {
        service = new AdminLeaveBalanceService(leaveBalanceRepository, employeeRepository, leaveBalanceService, auditLogService);
    }

    // ─── getBalances ──────────────────────────────────────────────

    @Test
    void getBalances_forbidden_throwsWhenNotAdmin() {
        assertThatThrownBy(() -> service.getBalances(employee, 2026, null, 0, 20))
                .isInstanceOf(ForbiddenApplicationException.class);
    }

    @Test
    void getBalances_returnsPagedResults() {
        LeaveBalanceEntity balance = LeaveBalanceEntity.create(2L, 2026, LeaveType.ANNUAL, 7200);
        setField(balance, "id", 1L);

        EmployeeEntity emp = instantiate(EmployeeEntity.class);
        setField(emp, "id", 2L);
        setField(emp, "name", "陳大明");

        when(leaveBalanceRepository.findAllByYearOrderByEmployeeIdAndLeaveType(eq(2026), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(balance)));
        when(employeeRepository.findAllById(any())).thenReturn(List.of(emp));

        PageResponse<AdminLeaveBalanceResponse> result = service.getBalances(admin, 2026, null, 0, 20);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).employeeName()).isEqualTo("陳大明");
        assertThat(result.items().get(0).quotaMinutes()).isEqualTo(7200);
        assertThat(result.items().get(0).leaveType()).isEqualTo("ANNUAL");
    }

    @Test
    void getBalances_filteredByEmployeeId_usesPagedQuery() {
        LeaveBalanceEntity balance = LeaveBalanceEntity.create(7L, 2026, LeaveType.SICK, 14400);
        setField(balance, "id", 5L);

        EmployeeEntity emp = instantiate(EmployeeEntity.class);
        setField(emp, "id", 7L);
        setField(emp, "name", "黃雅婷");
        PageRequest pageable = PageRequest.of(1, 2);

        when(leaveBalanceRepository.findAllByEmployeeIdAndYearOrderByLeaveType(7L, 2026, pageable))
                .thenReturn(new PageImpl<>(List.of(balance), pageable, 3));
        when(employeeRepository.findAllById(any())).thenReturn(List.of(emp));

        PageResponse<AdminLeaveBalanceResponse> result = service.getBalances(admin, 2026, 7L, 1, 2);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).employeeName()).isEqualTo("黃雅婷");
        assertThat(result.currentPage()).isEqualTo(1);
        assertThat(result.pageSize()).isEqualTo(2);
        assertThat(result.totalCount()).isEqualTo(3);
    }

    // ─── adjustBalance ────────────────────────────────────────────

    @Test
    void adjustBalance_forbidden_throwsWhenNotAdmin() {
        assertThatThrownBy(() -> service.adjustBalance(employee, 1L, new AdjustLeaveBalanceRequest(7200, "reason")))
                .isInstanceOf(ForbiddenApplicationException.class);
    }

    @Test
    void adjustBalance_setsNewQuota() {
        LeaveBalanceEntity balance = LeaveBalanceEntity.create(2L, 2026, LeaveType.ANNUAL, 7200);
        setField(balance, "id", 1L);
        balance.addUsed(480);

        EmployeeEntity actorEntity = instantiate(EmployeeEntity.class);
        setField(actorEntity, "id", 1L);
        EmployeeEntity balanceOwner = instantiate(EmployeeEntity.class);
        setField(balanceOwner, "id", 2L);
        setField(balanceOwner, "name", "陳大明");

        when(leaveBalanceRepository.findById(1L)).thenReturn(Optional.of(balance));
        when(employeeRepository.getReferenceById(1L)).thenReturn(actorEntity);
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(balanceOwner));

        AdminLeaveBalanceResponse response = service.adjustBalance(admin, 1L, new AdjustLeaveBalanceRequest(8640, "promotion bonus"));

        assertThat(response.quotaMinutes()).isEqualTo(8640);
        verify(auditLogService).log(
                eq(AuditEntityTypes.LEAVE_BALANCE),
                eq(1L),
                eq("ADJUST"),
                eq(actorEntity),
                eq(Map.of("oldQuotaMinutes", 7200, "newQuotaMinutes", 8640, "reason", "promotion bonus"))
        );
    }

    @Test
    void adjustBalance_keepsAuditDetailStructuredForSpecialCharacters() {
        LeaveBalanceEntity balance = LeaveBalanceEntity.create(2L, 2026, LeaveType.ANNUAL, 7200);
        setField(balance, "id", 1L);
        EmployeeEntity actorEntity = instantiate(EmployeeEntity.class);
        setField(actorEntity, "id", 1L);
        EmployeeEntity balanceOwner = instantiate(EmployeeEntity.class);
        setField(balanceOwner, "id", 2L);
        setField(balanceOwner, "name", "陳大明");
        String reason = "quota \"bonus\" \\ carryover\nmanual adjustment";

        when(leaveBalanceRepository.findById(1L)).thenReturn(Optional.of(balance));
        when(employeeRepository.getReferenceById(1L)).thenReturn(actorEntity);
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(balanceOwner));

        service.adjustBalance(admin, 1L, new AdjustLeaveBalanceRequest(8640, reason));

        ArgumentCaptor<Object> detailCaptor = ArgumentCaptor.forClass(Object.class);
        verify(auditLogService).log(
                eq(AuditEntityTypes.LEAVE_BALANCE),
                eq(1L),
                eq("ADJUST"),
                eq(actorEntity),
                detailCaptor.capture()
        );
        assertThat(detailCaptor.getValue()).isEqualTo(Map.of(
                "oldQuotaMinutes", 7200,
                "newQuotaMinutes", 8640,
                "reason", reason
        ));
    }

    @Test
    void adjustBalance_rejectsWhenQuotaBelowUsed() {
        LeaveBalanceEntity balance = LeaveBalanceEntity.create(2L, 2026, LeaveType.ANNUAL, 7200);
        setField(balance, "id", 1L);
        balance.addUsed(3360);

        when(leaveBalanceRepository.findById(1L)).thenReturn(Optional.of(balance));

        assertThatThrownBy(() -> service.adjustBalance(admin, 1L, new AdjustLeaveBalanceRequest(480, "error")))
                .isInstanceOf(BadRequestApplicationException.class);
    }

    // ─── initYearBalances ─────────────────────────────────────────

    @Test
    void initYearBalances_forbidden_throwsWhenNotAdmin() {
        assertThatThrownBy(() -> service.initYearBalances(employee, new InitYearBalancesRequest(2027)))
                .isInstanceOf(ForbiddenApplicationException.class);
    }

    @Test
    void initYearBalances_createsForAllEmployees() {
        EmployeeEntity emp1 = instantiate(EmployeeEntity.class);
        setField(emp1, "id", 1L);
        setField(emp1, "hireDate", LocalDate.of(2020, 1, 6));
        EmployeeEntity emp2 = instantiate(EmployeeEntity.class);
        setField(emp2, "id", 2L);
        setField(emp2, "hireDate", LocalDate.of(2022, 3, 1));

        when(employeeRepository.findAll()).thenReturn(List.of(emp1, emp2));
        when(leaveBalanceService.calculateQuotaMinutes(any(), any(), anyInt())).thenReturn(7200);
        when(leaveBalanceRepository.insertIfAbsent(anyLong(), anyInt(), anyString(), anyInt())).thenReturn(1);

        YearInitResult result = service.initYearBalances(admin, new InitYearBalancesRequest(2027));

        // 2 employees × 3 leave types = 6 inserts
        assertThat(result.initialized()).isEqualTo(6);
        assertThat(result.skipped()).isEqualTo(0);
        verify(leaveBalanceRepository, times(6)).insertIfAbsent(anyLong(), anyInt(), anyString(), anyInt());
    }

    @Test
    void initYearBalances_isIdempotent() {
        EmployeeEntity emp = instantiate(EmployeeEntity.class);
        setField(emp, "id", 1L);
        setField(emp, "hireDate", LocalDate.of(2020, 1, 6));

        when(employeeRepository.findAll()).thenReturn(List.of(emp));
        when(leaveBalanceService.calculateQuotaMinutes(any(), any(), anyInt())).thenReturn(7200);
        // All 3 inserts return 0 (already exist)
        when(leaveBalanceRepository.insertIfAbsent(anyLong(), anyInt(), anyString(), anyInt())).thenReturn(0);

        YearInitResult result = service.initYearBalances(admin, new InitYearBalancesRequest(2026));

        assertThat(result.initialized()).isEqualTo(0);
        assertThat(result.skipped()).isEqualTo(3);
    }

    // ─── helpers ─────────────────────────────────────────────────

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private <T> T instantiate(Class<T> type) {
        try {
            java.lang.reflect.Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
