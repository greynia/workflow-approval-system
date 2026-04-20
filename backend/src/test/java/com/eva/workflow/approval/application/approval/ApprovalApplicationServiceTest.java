package com.eva.workflow.approval.application.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eva.workflow.approval.api.dto.approval.ApprovalDecisionRequest;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.application.exception.BadRequestApplicationException;
import com.eva.workflow.approval.application.exception.ForbiddenApplicationException;
import com.eva.workflow.approval.application.exception.ResourceNotFoundApplicationException;
import com.eva.workflow.approval.application.request.LeaveBalanceService;
import com.eva.workflow.approval.common.enums.ActionType;
import com.eva.workflow.approval.common.enums.ApprovalStepType;
import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.common.enums.RequestStatus;
import com.eva.workflow.approval.common.enums.StepStatus;
import com.eva.workflow.approval.common.enums.UserRole;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.ApprovalActionEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.ApprovalStepEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.LeaveRequestEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.ApprovalActionRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.ApprovalStepRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ApprovalApplicationServiceTest {

    @Mock
    private ApprovalStepRepository approvalStepRepository;

    @Mock
    private ApprovalActionRepository approvalActionRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private LeaveBalanceService leaveBalanceService;

    private ApprovalApplicationService approvalApplicationService;

    @BeforeEach
    void setUp() {
        approvalApplicationService = new ApprovalApplicationService(
                approvalStepRepository,
                approvalActionRepository,
                auditLogRepository,
                objectMapper,
                leaveBalanceService
        );
    }

    @Test
    void approveStepMarksStepApprovedAndRequestApprovedWhenItIsLastPendingStep() throws JsonProcessingException {
        EmployeeEntity manager = employee(5L);
        LeaveRequestEntity leaveRequest = leaveRequest(100L, RequestStatus.PENDING);
        ApprovalStepEntity step = approvalStep(200L, 1, manager, leaveRequest, StepStatus.PENDING);

        when(approvalStepRepository.findWithRequestAndApproverById(200L)).thenReturn(Optional.of(step));
        when(approvalStepRepository.findByLeaveRequestIdOrderByStepOrderAsc(100L)).thenReturn(List.of(step));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        approvalApplicationService.approveStep(
                200L,
                authenticatedEmployee(5L),
                new ApprovalDecisionRequest("looks good")
        );

        assertThat(step.getStatus()).isEqualTo(StepStatus.APPROVED);
        assertThat(leaveRequest.getStatus()).isEqualTo(RequestStatus.APPROVED);

        ArgumentCaptor<ApprovalActionEntity> actionCaptor = ArgumentCaptor.forClass(ApprovalActionEntity.class);
        verify(approvalActionRepository).save(actionCaptor.capture());
        assertThat(actionCaptor.getValue().getActionType()).isEqualTo(ActionType.APPROVE);
        assertThat(actionCaptor.getValue().getComment()).isEqualTo("looks good");
    }

    @Test
    void approveStepKeepsRequestPendingWhenOtherStepsRemain() throws JsonProcessingException {
        EmployeeEntity manager = employee(5L);
        EmployeeEntity director = employee(2L);
        LeaveRequestEntity leaveRequest = leaveRequest(100L, RequestStatus.PENDING);
        ApprovalStepEntity firstStep = approvalStep(200L, 1, manager, leaveRequest, StepStatus.PENDING);
        ApprovalStepEntity secondStep = approvalStep(201L, 2, director, leaveRequest, StepStatus.PENDING);

        when(approvalStepRepository.findWithRequestAndApproverById(200L)).thenReturn(Optional.of(firstStep));
        when(approvalStepRepository.findByLeaveRequestIdOrderByStepOrderAsc(100L)).thenReturn(List.of(firstStep, secondStep));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        approvalApplicationService.approveStep(
                200L,
                authenticatedEmployee(5L),
                new ApprovalDecisionRequest(null)
        );

        assertThat(firstStep.getStatus()).isEqualTo(StepStatus.APPROVED);
        assertThat(secondStep.getStatus()).isEqualTo(StepStatus.PENDING);
        assertThat(leaveRequest.getStatus()).isEqualTo(RequestStatus.PENDING);
    }

    @Test
    void rejectStepMarksRequestRejectedAndSkipsRemainingPendingSteps() throws JsonProcessingException {
        EmployeeEntity manager = employee(5L);
        EmployeeEntity director = employee(2L);
        LeaveRequestEntity leaveRequest = leaveRequest(100L, RequestStatus.PENDING);
        ApprovalStepEntity firstStep = approvalStep(200L, 1, manager, leaveRequest, StepStatus.PENDING);
        ApprovalStepEntity secondStep = approvalStep(201L, 2, director, leaveRequest, StepStatus.PENDING);

        when(approvalStepRepository.findWithRequestAndApproverById(200L)).thenReturn(Optional.of(firstStep));
        when(approvalStepRepository.findByLeaveRequestIdOrderByStepOrderAsc(100L)).thenReturn(List.of(firstStep, secondStep));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        approvalApplicationService.rejectStep(
                200L,
                authenticatedEmployee(5L),
                new ApprovalDecisionRequest("not acceptable")
        );

        assertThat(firstStep.getStatus()).isEqualTo(StepStatus.REJECTED);
        assertThat(secondStep.getStatus()).isEqualTo(StepStatus.SKIPPED);
        assertThat(leaveRequest.getStatus()).isEqualTo(RequestStatus.REJECTED);
    }

    @Test
    void decisionFailsWhenActorIsNotApprover() {
        EmployeeEntity manager = employee(5L);
        LeaveRequestEntity leaveRequest = leaveRequest(100L, RequestStatus.PENDING);
        ApprovalStepEntity step = approvalStep(200L, 1, manager, leaveRequest, StepStatus.PENDING);

        when(approvalStepRepository.findWithRequestAndApproverById(200L)).thenReturn(Optional.of(step));
        when(approvalStepRepository.findByLeaveRequestIdOrderByStepOrderAsc(100L)).thenReturn(List.of(step));

        assertThatThrownBy(() -> approvalApplicationService.approveStep(
                200L,
                authenticatedEmployee(9L),
                new ApprovalDecisionRequest(null)
        ))
                .isInstanceOf(ForbiddenApplicationException.class)
                .hasMessageContaining("not allowed");

        verify(approvalActionRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void decisionFailsWhenStepNotFound() {
        when(approvalStepRepository.findWithRequestAndApproverById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> approvalApplicationService.approveStep(
                999L,
                authenticatedEmployee(5L),
                new ApprovalDecisionRequest(null)
        ))
                .isInstanceOf(ResourceNotFoundApplicationException.class)
                .hasMessageContaining("Approval step not found");

        verify(approvalActionRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void decisionFailsWhenStepHasAlreadyBeenProcessed() {
        EmployeeEntity manager = employee(5L);
        LeaveRequestEntity leaveRequest = leaveRequest(100L, RequestStatus.PENDING);
        ApprovalStepEntity step = approvalStep(200L, 1, manager, leaveRequest, StepStatus.APPROVED);

        when(approvalStepRepository.findWithRequestAndApproverById(200L)).thenReturn(Optional.of(step));
        when(approvalStepRepository.findByLeaveRequestIdOrderByStepOrderAsc(100L)).thenReturn(List.of(step));

        assertThatThrownBy(() -> approvalApplicationService.rejectStep(
                200L,
                authenticatedEmployee(5L),
                new ApprovalDecisionRequest("too late")
        ))
                .isInstanceOf(BadRequestApplicationException.class)
                .hasMessageContaining("already been processed");

        verify(approvalActionRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void decisionFailsWhenPreviousApprovalStepHasNotBeenCompleted() {
        EmployeeEntity firstManager = employee(5L);
        EmployeeEntity secondManager = employee(2L);
        LeaveRequestEntity leaveRequest = leaveRequest(100L, RequestStatus.PENDING);
        ApprovalStepEntity firstStep = approvalStep(200L, 1, firstManager, leaveRequest, StepStatus.PENDING);
        ApprovalStepEntity secondStep = approvalStep(201L, 2, secondManager, leaveRequest, StepStatus.PENDING);

        when(approvalStepRepository.findWithRequestAndApproverById(201L)).thenReturn(Optional.of(secondStep));
        when(approvalStepRepository.findByLeaveRequestIdOrderByStepOrderAsc(100L)).thenReturn(List.of(firstStep, secondStep));

        assertThatThrownBy(() -> approvalApplicationService.approveStep(
                201L,
                authenticatedEmployee(2L),
                new ApprovalDecisionRequest(null)
        ))
                .isInstanceOf(BadRequestApplicationException.class)
                .hasMessageContaining("Previous approval step has not been completed");

        verify(approvalActionRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    private AuthenticatedEmployee authenticatedEmployee(Long employeeId) {
        return new AuthenticatedEmployee(employeeId, "user@example.com", "User", UserRole.MANAGER, java.util.List.of());
    }

    private EmployeeEntity employee(Long id) {
        EmployeeEntity entity = newInstance(EmployeeEntity.class);
        setField(entity, "id", id);
        return entity;
    }

    private LeaveRequestEntity leaveRequest(Long id, RequestStatus status) {
        LeaveRequestEntity entity = LeaveRequestEntity.create(
                employee(7L),
                null,
                LeaveType.ANNUAL,
                LocalDateTime.of(2026, 4, 20, 9, 0),
                LocalDateTime.of(2026, 4, 22, 18, 0),
                1440,
                "Leave",
                status
        );
        setField(entity, "id", id);
        return entity;
    }

    private ApprovalStepEntity approvalStep(
            Long id,
            int order,
            EmployeeEntity approver,
            LeaveRequestEntity leaveRequest,
            StepStatus status
    ) {
        ApprovalStepEntity entity = ApprovalStepEntity.create(
                leaveRequest,
                order,
                approver,
                ApprovalStepType.MANAGER,
                status
        );
        setField(entity, "id", id);
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
