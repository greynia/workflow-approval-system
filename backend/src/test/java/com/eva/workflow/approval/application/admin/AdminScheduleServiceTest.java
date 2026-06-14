package com.eva.workflow.approval.application.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.eva.workflow.approval.api.dto.admin.CompanyWorkScheduleResponse;
import com.eva.workflow.approval.api.dto.admin.EmployeeScheduleResponse;
import com.eva.workflow.approval.api.dto.common.PageResponse;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.application.exception.ForbiddenApplicationException;
import com.eva.workflow.approval.application.request.CompanyWorkScheduleService;
import com.eva.workflow.approval.common.enums.UserRole;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.CompanyWorkScheduleEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeScheduleEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.EmployeeScheduleRepository;

@ExtendWith(MockitoExtension.class)
class AdminScheduleServiceTest {

    @Mock
    private CompanyWorkScheduleService companyWorkScheduleService;

    @Mock
    private EmployeeScheduleRepository employeeScheduleRepository;

    private AdminScheduleService adminScheduleService;

    private final AuthenticatedEmployee admin = new AuthenticatedEmployee(1L, "admin@example.com", "Admin", UserRole.ADMIN, List.of());
    private final AuthenticatedEmployee employee = new AuthenticatedEmployee(7L, "user@example.com", "User", UserRole.EMPLOYEE, List.of());

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-05-04T00:00:00Z"), ZoneOffset.UTC);
        adminScheduleService = new AdminScheduleService(companyWorkScheduleService, employeeScheduleRepository, fixedClock);
    }

    @Test
    void getCompanyWorkSchedule_usesInjectedClockDate() {
        CompanyWorkScheduleEntity schedule = companyWorkScheduleEntity();
        when(companyWorkScheduleService.getEffectiveSchedule(LocalDate.of(2026, 5, 4))).thenReturn(schedule);

        CompanyWorkScheduleResponse response = adminScheduleService.getCompanyWorkSchedule(admin);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.workStart()).isEqualTo(LocalTime.of(9, 0));
        verify(companyWorkScheduleService).getEffectiveSchedule(LocalDate.of(2026, 5, 4));
    }

    @Test
    void getEmployeeSchedules_rejectsNonAdmin() {
        assertThatThrownBy(() -> adminScheduleService.getEmployeeSchedules(employee, 0, 20))
                .isInstanceOf(ForbiddenApplicationException.class);
    }

    @Test
    void getEmployeeSchedules_returnsPagedEmployeeData() {
        EmployeeScheduleEntity schedule = employeeScheduleEntity();
        PageRequest pageable = PageRequest.of(0, 5);
        when(employeeScheduleRepository.findAllWithEmployee(pageable))
                .thenReturn(new PageImpl<>(List.of(schedule), pageable, 9));

        PageResponse<EmployeeScheduleResponse> response = adminScheduleService.getEmployeeSchedules(admin, 0, 5);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).employeeName()).isEqualTo("王小明");
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.pageSize()).isEqualTo(5);
        assertThat(response.totalCount()).isEqualTo(9);
        verify(employeeScheduleRepository).findAllWithEmployee(pageable);
    }

    private CompanyWorkScheduleEntity companyWorkScheduleEntity() {
        CompanyWorkScheduleEntity entity = instantiate(CompanyWorkScheduleEntity.class);
        setField(entity, "id", 1L);
        setField(entity, "workStart", LocalTime.of(9, 0));
        setField(entity, "workEnd", LocalTime.of(18, 0));
        setField(entity, "lunchStart", LocalTime.of(12, 0));
        setField(entity, "lunchEnd", LocalTime.of(13, 0));
        setField(entity, "workDays", "[1,2,3,4,5]");
        setField(entity, "effectiveFrom", LocalDate.of(2020, 1, 1));
        return entity;
    }

    private EmployeeScheduleEntity employeeScheduleEntity() {
        EmployeeEntity employee = instantiate(EmployeeEntity.class);
        setField(employee, "id", 7L);
        setField(employee, "name", "王小明");
        setField(employee, "employeeNo", "E007");

        EmployeeScheduleEntity schedule = instantiate(EmployeeScheduleEntity.class);
        setField(schedule, "id", 11L);
        setField(schedule, "employee", employee);
        setField(schedule, "scheduleType", "STANDARD");
        setField(schedule, "effectiveFrom", LocalDate.of(2024, 1, 2));
        return schedule;
    }

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
