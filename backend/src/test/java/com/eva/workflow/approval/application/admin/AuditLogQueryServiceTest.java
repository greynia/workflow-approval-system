package com.eva.workflow.approval.application.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.eva.workflow.approval.api.dto.admin.AuditLogResponse;
import com.eva.workflow.approval.api.dto.common.PageResponse;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.application.exception.BadRequestApplicationException;
import com.eva.workflow.approval.application.exception.ForbiddenApplicationException;
import com.eva.workflow.approval.common.enums.UserRole;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.AuditLogEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.AuditLogRepository;

@ExtendWith(MockitoExtension.class)
class AuditLogQueryServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogQueryService auditLogQueryService;

    @BeforeEach
    void setUp() {
        auditLogQueryService = new AuditLogQueryService(auditLogRepository);
    }

    @Test
    void getAuditLogsRejectsNonAdminUsers() {
        AuthenticatedEmployee employee = new AuthenticatedEmployee(7L, "user@example.com", "User", UserRole.EMPLOYEE);

        assertThatThrownBy(() -> auditLogQueryService.getAuditLogs(
                employee,
                null,
                null,
                null,
                null,
                null,
                0,
                20
        )).isInstanceOf(ForbiddenApplicationException.class)
                .hasMessageContaining("Only admins");
    }

    @Test
    void getAuditLogsRejectsInvalidDateRange() {
        AuthenticatedEmployee admin = new AuthenticatedEmployee(1L, "admin@example.com", "Admin", UserRole.ADMIN);
        LocalDateTime createdFrom = LocalDateTime.of(2026, 4, 17, 0, 0);
        LocalDateTime createdTo = LocalDateTime.of(2026, 4, 16, 0, 0);

        assertThatThrownBy(() -> auditLogQueryService.getAuditLogs(
                admin,
                null,
                null,
                null,
                createdFrom,
                createdTo,
                0,
                20
        )).isInstanceOf(BadRequestApplicationException.class)
                .hasMessageContaining("createdFrom");
    }

    @Test
    void getAuditLogsMapsPageResult() {
        AuthenticatedEmployee admin = new AuthenticatedEmployee(1L, "admin@example.com", "Admin", UserRole.ADMIN);
        EmployeeEntity actor = employeeEntity(6L, "李建國");
        AuditLogEntity auditLog = auditLogEntity(
                101L,
                "LEAVE_REQUEST",
                88L,
                "APPROVE",
                actor,
                "{\"stepId\": 5}",
                LocalDateTime.of(2026, 4, 16, 12, 30)
        );
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        when(auditLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(auditLog), pageable, 1));

        PageResponse<AuditLogResponse> response = auditLogQueryService.getAuditLogs(
                admin,
                " LEAVE_REQUEST ",
                " APPROVE ",
                "李建國",
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 4, 30, 23, 59),
                0,
                20
        );

        assertThat(response.items()).hasSize(1);
        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.currentPage()).isZero();
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.items().get(0)).isEqualTo(new AuditLogResponse(
                101L,
                "LEAVE_REQUEST",
                88L,
                "APPROVE",
                6L,
                "李建國",
                "{\"stepId\": 5}",
                LocalDateTime.of(2026, 4, 16, 12, 30)
        ));

        verify(auditLogRepository).findAll(any(Specification.class), eq(pageable));
    }

    private AuditLogEntity auditLogEntity(
            Long id,
            String entityType,
            Long entityId,
            String action,
            EmployeeEntity actor,
            String detailJson,
            LocalDateTime createdAt
    ) {
        AuditLogEntity entity = newInstance(AuditLogEntity.class);
        setField(entity, "id", id);
        setField(entity, "entityType", entityType);
        setField(entity, "entityId", entityId);
        setField(entity, "action", action);
        setField(entity, "actor", actor);
        setField(entity, "detailJson", detailJson);
        setField(entity, "createdAt", createdAt);
        return entity;
    }

    private EmployeeEntity employeeEntity(Long id, String name) {
        EmployeeEntity entity = newInstance(EmployeeEntity.class);
        setField(entity, "id", id);
        setField(entity, "name", name);
        return entity;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private <T> T newInstance(Class<T> type) {
        try {
            java.lang.reflect.Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
