package com.eva.workflow.approval.application.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.eva.workflow.approval.api.dto.leave.ApprovalStepResponse;
import com.eva.workflow.approval.api.dto.leave.CreateLeaveRequest;
import com.eva.workflow.approval.api.dto.leave.LeaveRequestDetailResponse;
import com.eva.workflow.approval.application.audit.AuditLogService;
import com.eva.workflow.approval.application.exception.ForbiddenApplicationException;
import com.eva.workflow.approval.application.exception.ResourceNotFoundApplicationException;
import com.eva.workflow.approval.application.approval.ManagerChainResolver;
import com.eva.workflow.approval.application.approval.WorkflowRuleAssembler;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.application.exception.BadRequestApplicationException;
import com.eva.workflow.approval.common.AuditEntityTypes;
import com.eva.workflow.approval.common.enums.ApprovalStepType;
import com.eva.workflow.approval.common.enums.LeaveRequestStage;
import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.common.enums.RequestStatus;
import com.eva.workflow.approval.common.enums.StepStatus;
import com.eva.workflow.approval.common.enums.UserRole;
import com.eva.workflow.approval.domain.approval.model.ApprovalFlowStep;
import com.eva.workflow.approval.domain.approval.model.ApprovalRule;
import com.eva.workflow.approval.domain.approval.service.ApprovalFlowEngine;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.ApprovalStepEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.LeaveRequestEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.ApprovalActionRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.ApprovalStepRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.EmployeeRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.LeaveRequestRepository;

@ExtendWith(MockitoExtension.class)
class LeaveRequestApplicationServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private ApprovalStepRepository approvalStepRepository;
    @Mock
    private ApprovalActionRepository approvalActionRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private LeaveRequestMapper leaveRequestMapper;
    @Mock
    private WorkflowRuleAssembler workflowRuleAssembler;
    @Mock
    private ManagerChainResolver managerChainResolver;
    @Mock
    private ApprovalFlowEngine approvalFlowEngine;
    @Mock
    private LeaveQuotaEngine leaveQuotaEngine;
    @Mock
    private LeaveBalanceService leaveBalanceService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private LeaveRequestApplicationService leaveRequestApplicationService;

    @BeforeEach
    void setUp() {
        leaveRequestApplicationService = new LeaveRequestApplicationService(
                leaveRequestRepository,
                employeeRepository,
                approvalStepRepository,
                approvalActionRepository,
                auditLogService,
                leaveRequestMapper,
                workflowRuleAssembler,
                managerChainResolver,
                approvalFlowEngine,
                leaveQuotaEngine,
                leaveBalanceService,
                applicationEventPublisher
        );
    }

    @Test
    void createRequestPersistsDeputyAndManagerStepsFromDomainOutput() {
        AuthenticatedEmployee authenticatedEmployee = new AuthenticatedEmployee(7L, "huang.yating@example.com", "黃雅婷", UserRole.EMPLOYEE, java.util.List.of());
        LocalDateTime startTime = LocalDateTime.of(2026, 4, 20, 9, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 4, 22, 18, 0);
        CreateLeaveRequest request = new CreateLeaveRequest(LeaveType.ANNUAL, startTime, endTime, "Family trip", 6L);

        EmployeeEntity applicant = employee(7L, true);
        EmployeeEntity deputy = employee(6L, true);
        EmployeeEntity manager = employee(5L, true);
        LeaveRequestEntity savedLeaveRequest = leaveRequest(100L, applicant, deputy, 1440, startTime, endTime);
        LeaveRequestDetailResponse expectedResponse = new LeaveRequestDetailResponse(
                100L, 7L, "黃雅婷", 6L, "李建國", LeaveType.ANNUAL, startTime, endTime, 1440,
                "Family trip", RequestStatus.PENDING, LeaveRequestStage.WAITING_DEPUTY,
                Instant.parse("2026-04-14T10:00:00Z"), Instant.parse("2026-04-14T10:00:00Z"), List.of(), List.of()
        );

        when(employeeRepository.findById(7L)).thenReturn(Optional.of(applicant));
        when(employeeRepository.findById(6L)).thenReturn(Optional.of(deputy));
        when(leaveQuotaEngine.calculateDurationMinutes(7L, startTime, endTime)).thenReturn(1440);
        when(leaveRequestRepository.existsOverlappingLeave(7L, startTime, endTime, List.of(RequestStatus.APPROVED, RequestStatus.PENDING)))
                .thenReturn(false);
        when(leaveRequestRepository.existsOverlappingLeave(6L, startTime, endTime, List.of(RequestStatus.APPROVED, RequestStatus.PENDING)))
                .thenReturn(false);
        when(leaveRequestRepository.save(any(LeaveRequestEntity.class))).thenReturn(savedLeaveRequest);
        when(workflowRuleAssembler.loadActiveWorkflowRules()).thenReturn(List.of(new ApprovalRule(null, 1440, null, 1)));
        when(managerChainResolver.resolveEligibleManagerChainIds(applicant)).thenReturn(List.of(5L));
        when(approvalFlowEngine.generateSteps(any())).thenReturn(List.of(
                new ApprovalFlowStep(1, 6L, ApprovalStepType.DEPUTY),
                new ApprovalFlowStep(2, 5L, ApprovalStepType.MANAGER)
        ));
        when(employeeRepository.findAllById(List.of(6L, 5L))).thenReturn(List.of(deputy, manager));
        when(approvalStepRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(leaveRequestMapper.toStepResponse(any(ApprovalStepEntity.class))).thenAnswer(invocation -> {
            ApprovalStepEntity step = invocation.getArgument(0);
            return new ApprovalStepResponse(
                    null,
                    step.getApprover().getId(),
                    null,
                    step.getStepType(),
                    step.getStatus(),
                    null,
                    null
            );
        });
        when(leaveRequestMapper.toDetailResponse(eq(savedLeaveRequest), any(), eq(List.of()))).thenReturn(expectedResponse);
        LeaveRequestDetailResponse response = leaveRequestApplicationService.createRequest(authenticatedEmployee, request);

        ArgumentCaptor<List> approvalStepsCaptor = ArgumentCaptor.forClass(List.class);
        verify(approvalStepRepository).saveAll(approvalStepsCaptor.capture());
        @SuppressWarnings("unchecked")
        List<ApprovalStepEntity> approvalSteps = approvalStepsCaptor.getValue();

        assertThat(approvalSteps).hasSize(2);
        assertThat(approvalSteps.get(0).getStepType()).isEqualTo(ApprovalStepType.DEPUTY);
        assertThat(approvalSteps.get(0).getApprover().getId()).isEqualTo(6L);
        assertThat(approvalSteps.get(1).getStepType()).isEqualTo(ApprovalStepType.MANAGER);
        assertThat(approvalSteps.get(1).getApprover().getId()).isEqualTo(5L);
        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void createRequestRejectsDeputyWhenDeputyHasOverlappingLeave() {
        AuthenticatedEmployee authenticatedEmployee = new AuthenticatedEmployee(7L, "huang.yating@example.com", "黃雅婷", UserRole.EMPLOYEE, java.util.List.of());
        LocalDateTime startTime = LocalDateTime.of(2026, 4, 20, 9, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 4, 22, 18, 0);
        CreateLeaveRequest request = new CreateLeaveRequest(LeaveType.ANNUAL, startTime, endTime, "Family trip", 6L);

        EmployeeEntity applicant = employee(7L, true);
        EmployeeEntity deputy = employee(6L, true);

        when(employeeRepository.findById(7L)).thenReturn(Optional.of(applicant));
        when(employeeRepository.findById(6L)).thenReturn(Optional.of(deputy));
        when(leaveQuotaEngine.calculateDurationMinutes(7L, startTime, endTime)).thenReturn(1440);
        when(leaveRequestRepository.existsOverlappingLeave(7L, startTime, endTime, List.of(RequestStatus.APPROVED, RequestStatus.PENDING)))
                .thenReturn(false);
        when(leaveRequestRepository.existsOverlappingLeave(6L, startTime, endTime, List.of(RequestStatus.APPROVED, RequestStatus.PENDING)))
                .thenReturn(true);

        assertThatThrownBy(() -> leaveRequestApplicationService.createRequest(authenticatedEmployee, request))
                .isInstanceOf(BadRequestApplicationException.class)
                .hasMessageContaining("DEPUTY_ON_LEAVE");
    }

    @Test
    void createRequestRejectsApplicantWhenApplicantHasOverlappingLeave() {
        AuthenticatedEmployee authenticatedEmployee = new AuthenticatedEmployee(7L, "huang.yating@example.com", "黃雅婷", UserRole.EMPLOYEE, java.util.List.of());
        LocalDateTime startTime = LocalDateTime.of(2026, 4, 21, 9, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 4, 21, 18, 0);
        CreateLeaveRequest request = new CreateLeaveRequest(LeaveType.ANNUAL, startTime, endTime, "Overlap", 6L);

        EmployeeEntity applicant = employee(7L, true);

        when(employeeRepository.findById(7L)).thenReturn(Optional.of(applicant));
        when(leaveQuotaEngine.calculateDurationMinutes(7L, startTime, endTime)).thenReturn(480);
        when(leaveRequestRepository.existsOverlappingLeave(7L, startTime, endTime, List.of(RequestStatus.APPROVED, RequestStatus.PENDING)))
                .thenReturn(true);

        assertThatThrownBy(() -> leaveRequestApplicationService.createRequest(authenticatedEmployee, request))
                .isInstanceOf(BadRequestApplicationException.class)
                .hasMessageContaining("APPLICANT_ON_LEAVE");
    }

    @Test
    void createRequestRejectsDeputySameAsApplicant() {
        AuthenticatedEmployee authenticatedEmployee = new AuthenticatedEmployee(7L, "huang.yating@example.com", "黃雅婷", UserRole.EMPLOYEE, java.util.List.of());
        LocalDateTime startTime = LocalDateTime.of(2026, 4, 21, 9, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 4, 21, 18, 0);
        CreateLeaveRequest request = new CreateLeaveRequest(LeaveType.ANNUAL, startTime, endTime, "Same deputy", 7L);

        EmployeeEntity applicant = employee(7L, true);

        when(employeeRepository.findById(7L)).thenReturn(Optional.of(applicant));
        when(leaveQuotaEngine.calculateDurationMinutes(7L, startTime, endTime)).thenReturn(480);
        when(leaveRequestRepository.existsOverlappingLeave(7L, startTime, endTime, List.of(RequestStatus.APPROVED, RequestStatus.PENDING)))
                .thenReturn(false);

        assertThatThrownBy(() -> leaveRequestApplicationService.createRequest(authenticatedEmployee, request))
                .isInstanceOf(BadRequestApplicationException.class)
                .hasMessageContaining("deputyId cannot be the same as applicant");
    }

    @Test
    void getRequestDetailAllowsAdminToViewAnyRequest() {
        AuthenticatedEmployee admin = new AuthenticatedEmployee(1L, "admin@example.com", "Admin", UserRole.ADMIN, java.util.List.of());
        EmployeeEntity applicant = employee(7L, true);
        EmployeeEntity deputy = employee(6L, true);
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 1, 9, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 5, 1, 18, 0);
        LeaveRequestEntity leaveRequest = leaveRequest(10L, applicant, deputy, 480, startTime, endTime);
        LeaveRequestDetailResponse stubResponse = new LeaveRequestDetailResponse(
                10L, 7L, "applicant", 6L, "deputy", LeaveType.ANNUAL,
                startTime, endTime, 480, "reason", RequestStatus.PENDING,
                LeaveRequestStage.WAITING_DEPUTY, Instant.parse("2026-05-01T01:00:00Z"), Instant.parse("2026-05-01T01:00:00Z"), List.of(), List.of());

        when(leaveRequestRepository.findById(10L)).thenReturn(Optional.of(leaveRequest));
        when(approvalStepRepository.findByLeaveRequestIdOrderByStepOrderAsc(10L)).thenReturn(List.of());
        when(approvalActionRepository.findByApprovalStepLeaveRequestIdOrderByCreatedAtAsc(10L)).thenReturn(List.of());
        when(leaveRequestMapper.toDetailResponse(eq(leaveRequest), any(), any())).thenReturn(stubResponse);

        LeaveRequestDetailResponse result = leaveRequestApplicationService.getRequestDetail(admin, 10L);

        assertThat(result).isEqualTo(stubResponse);
    }

    @Test
    void getRequestDetailRejectsUnrelatedEmployee() {
        AuthenticatedEmployee unrelated = new AuthenticatedEmployee(99L, "other@example.com", "Other", UserRole.EMPLOYEE, java.util.List.of());
        EmployeeEntity applicant = employee(7L, true);
        EmployeeEntity deputy = employee(6L, true);
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 1, 9, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 5, 1, 18, 0);
        LeaveRequestEntity leaveRequest = leaveRequest(10L, applicant, deputy, 480, startTime, endTime);

        when(leaveRequestRepository.findById(10L)).thenReturn(Optional.of(leaveRequest));
        when(approvalStepRepository.existsByLeaveRequestIdAndApproverId(10L, 99L)).thenReturn(false);

        assertThatThrownBy(() -> leaveRequestApplicationService.getRequestDetail(unrelated, 10L))
                .isInstanceOf(ResourceNotFoundApplicationException.class);
    }

    @Test
    void recallRequest_cancelsApprovedRequest() {
        AuthenticatedEmployee applicantPrincipal = new AuthenticatedEmployee(7L, "user@example.com", "User", UserRole.EMPLOYEE, java.util.List.of());
        EmployeeEntity applicant = employee(7L, true);
        EmployeeEntity deputy = employee(6L, true);
        LocalDateTime startTime = LocalDateTime.of(2026, 6, 1, 9, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 6, 1, 18, 0);
        LeaveRequestEntity leaveRequest = leaveRequest(20L, applicant, deputy, 480, startTime, endTime);
        leaveRequest.updateStatus(RequestStatus.APPROVED);

        when(leaveRequestRepository.findById(20L)).thenReturn(Optional.of(leaveRequest));

        leaveRequestApplicationService.recallRequest(applicantPrincipal, 20L);

        assertThat(leaveRequest.getStatus()).isEqualTo(RequestStatus.CANCELLED);
        verify(leaveBalanceService).refundBalance(7L, LeaveType.ANNUAL, 2026, 480);
        verify(auditLogService).log(eq(AuditEntityTypes.LEAVE_REQUEST), eq(20L), eq("RECALL"), eq(applicant), any());
    }

    @Test
    void recallRequest_rejectsWhenNotApplicant() {
        AuthenticatedEmployee other = new AuthenticatedEmployee(99L, "other@example.com", "Other", UserRole.EMPLOYEE, java.util.List.of());
        EmployeeEntity applicant = employee(7L, true);
        EmployeeEntity deputy = employee(6L, true);
        LocalDateTime startTime = LocalDateTime.of(2026, 6, 1, 9, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 6, 1, 18, 0);
        LeaveRequestEntity leaveRequest = leaveRequest(20L, applicant, deputy, 480, startTime, endTime);
        leaveRequest.updateStatus(RequestStatus.APPROVED);

        when(leaveRequestRepository.findById(20L)).thenReturn(Optional.of(leaveRequest));

        assertThatThrownBy(() -> leaveRequestApplicationService.recallRequest(other, 20L))
                .isInstanceOf(ForbiddenApplicationException.class);
    }

    @Test
    void recallRequest_rejectsWhenNotApproved() {
        AuthenticatedEmployee applicantPrincipal = new AuthenticatedEmployee(7L, "user@example.com", "User", UserRole.EMPLOYEE, java.util.List.of());
        EmployeeEntity applicant = employee(7L, true);
        EmployeeEntity deputy = employee(6L, true);
        LocalDateTime startTime = LocalDateTime.of(2026, 6, 1, 9, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 6, 1, 18, 0);
        LeaveRequestEntity leaveRequest = leaveRequest(20L, applicant, deputy, 480, startTime, endTime);
        // status is PENDING (default from factory)

        when(leaveRequestRepository.findById(20L)).thenReturn(Optional.of(leaveRequest));

        assertThatThrownBy(() -> leaveRequestApplicationService.recallRequest(applicantPrincipal, 20L))
                .isInstanceOf(BadRequestApplicationException.class);
    }

    private EmployeeEntity employee(Long id, boolean active) {
        EmployeeEntity entity = newInstance(EmployeeEntity.class);
        setField(entity, "id", id);
        setField(entity, "active", active);
        return entity;
    }

    private LeaveRequestEntity leaveRequest(Long id, EmployeeEntity applicant, EmployeeEntity deputy, int durationMinutes, LocalDateTime startTime, LocalDateTime endTime) {
        LeaveRequestEntity entity = LeaveRequestEntity.create(
                applicant,
                deputy,
                LeaveType.ANNUAL,
                startTime,
                endTime,
                durationMinutes,
                "Family trip",
                RequestStatus.PENDING
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
