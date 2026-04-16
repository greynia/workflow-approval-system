package com.eva.workflow.approval.application.request;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.api.dto.common.PageResponse;
import com.eva.workflow.approval.api.dto.leave.ApprovalActionResponse;
import com.eva.workflow.approval.api.dto.leave.ApprovalStepResponse;
import com.eva.workflow.approval.api.dto.leave.CreateLeaveRequest;
import com.eva.workflow.approval.api.dto.leave.LeaveBalanceResponse;
import com.eva.workflow.approval.api.dto.leave.PendingRequestCountResponse;
import com.eva.workflow.approval.api.dto.leave.LeaveCalculationRequest;
import com.eva.workflow.approval.api.dto.leave.LeaveCalculationResponse;
import com.eva.workflow.approval.api.dto.leave.LeaveRequestDetailResponse;
import com.eva.workflow.approval.api.dto.leave.LeaveRequestSummaryResponse;
import com.eva.workflow.approval.application.approval.ManagerChainResolver;
import com.eva.workflow.approval.application.approval.WorkflowRuleAssembler;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.application.exception.BadRequestApplicationException;
import com.eva.workflow.approval.application.exception.ForbiddenApplicationException;
import com.eva.workflow.approval.application.exception.ResourceNotFoundApplicationException;
import com.eva.workflow.approval.common.enums.StepStatus;
import com.eva.workflow.approval.common.enums.RequestStatus;
import com.eva.workflow.approval.common.enums.UserRole;
import com.eva.workflow.approval.domain.approval.model.ApprovalFlowContext;
import com.eva.workflow.approval.domain.approval.model.ApprovalFlowStep;
import com.eva.workflow.approval.domain.approval.model.ApprovalRule;
import com.eva.workflow.approval.domain.approval.service.ApprovalFlowEngine;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.ApprovalStepEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.AuditLogEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.LeaveRequestEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.ApprovalActionRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.ApprovalStepRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.AuditLogRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.EmployeeRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.LeaveRequestRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeaveRequestApplicationService {

    private static final String LEAVE_REQUEST_ENTITY_TYPE = "LEAVE_REQUEST";
    private static final String CREATE_LEAVE_REQUEST_ACTION = "CREATE";

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final ApprovalStepRepository approvalStepRepository;
    private final ApprovalActionRepository approvalActionRepository;
    private final AuditLogRepository auditLogRepository;
    private final LeaveRequestMapper leaveRequestMapper;
    private final ObjectMapper objectMapper;
    private final WorkflowRuleAssembler workflowRuleAssembler;
    private final ManagerChainResolver managerChainResolver;
    private final ApprovalFlowEngine approvalFlowEngine;
    private final LeaveQuotaEngine leaveQuotaEngine;
    private final LeaveBalanceService leaveBalanceService;

    @Transactional
    public LeaveRequestDetailResponse createRequest(AuthenticatedEmployee authenticatedEmployee, CreateLeaveRequest request) {
        validateCreateRequest(request);

        EmployeeEntity applicant = findActiveEmployee(authenticatedEmployee.employeeId(), "Applicant not found");
        int durationMinutes = leaveQuotaEngine.calculateDurationMinutes(applicant.getId(), request.startTime(), request.endTime());
        validateApplicantHasNoOverlappingLeave(applicant.getId(), request.startTime(), request.endTime());
        EmployeeEntity deputy = resolveDeputy(request.deputyId(), applicant.getId(), request.startTime(), request.endTime());

        leaveBalanceService.deductBalance(
                applicant.getId(),
                applicant.getHireDate(),
                request.type(),
                request.startTime().getYear(),
                durationMinutes
        );

        LeaveRequestEntity saved = leaveRequestRepository.save(LeaveRequestEntity.create(
                applicant,
                deputy,
                request.type(),
                request.startTime(),
                request.endTime(),
                durationMinutes,
                request.reason(),
                RequestStatus.PENDING
        ));

        List<ApprovalStepEntity> approvalSteps = createApprovalSteps(saved, applicant);
        List<ApprovalStepResponse> stepResponses = approvalSteps.stream()
                .map(leaveRequestMapper::toStepResponse)
                .toList();

        auditLogRepository.save(AuditLogEntity.create(
                LEAVE_REQUEST_ENTITY_TYPE,
                saved.getId(),
                CREATE_LEAVE_REQUEST_ACTION,
                applicant,
                buildCreateAuditDetail(saved)
        ));

        return leaveRequestMapper.toDetailResponse(saved, stepResponses, List.of());
    }

    @Transactional(readOnly = true)
    public LeaveCalculationResponse calculateDuration(
            AuthenticatedEmployee authenticatedEmployee,
            LeaveCalculationRequest request
    ) {
        int durationMinutes = leaveQuotaEngine.calculateDurationMinutes(
                authenticatedEmployee.employeeId(),
                request.startTime(),
                request.endTime()
        );
        return new LeaveCalculationResponse(durationMinutes);
    }

    @Transactional(readOnly = true)
    public PageResponse<LeaveRequestSummaryResponse> getRequests(
            AuthenticatedEmployee authenticatedEmployee,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LeaveRequestEntity> result = leaveRequestRepository.findByApplicantIdOrderByCreatedAtDesc(
                authenticatedEmployee.employeeId(),
                pageable
        );

        List<Long> requestIds = result.getContent().stream()
                .map(LeaveRequestEntity::getId)
                .toList();

        Map<Long, List<ApprovalStepEntity>> stepsByRequestId =
                approvalStepRepository.findGroupedByLeaveRequestIds(requestIds);

        return new PageResponse<>(
                result.getContent().stream()
                        .map(entity -> {
                            List<ApprovalStepResponse> approvalSteps = stepsByRequestId
                                    .getOrDefault(entity.getId(), List.of()).stream()
                                    .map(leaveRequestMapper::toStepResponse)
                                    .toList();
                            return leaveRequestMapper.toSummaryResponse(entity, approvalSteps);
                        })
                        .toList(),
                result.getNumber(),
                result.getTotalElements(),
                result.getSize(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public LeaveRequestDetailResponse getRequestDetail(AuthenticatedEmployee authenticatedEmployee, Long requestId) {
        LeaveRequestEntity leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundApplicationException("Leave request not found"));
        boolean canAccess = authenticatedEmployee.role() == UserRole.ADMIN
                || leaveRequest.getApplicant().getId().equals(authenticatedEmployee.employeeId())
                || approvalStepRepository.existsByLeaveRequestIdAndApproverId(requestId, authenticatedEmployee.employeeId());
        if (!canAccess) {
            throw new ResourceNotFoundApplicationException("Leave request not found");
        }

        List<ApprovalStepResponse> steps = approvalStepRepository.findByLeaveRequestIdOrderByStepOrderAsc(requestId).stream()
                .map(leaveRequestMapper::toStepResponse)
                .toList();
        List<ApprovalActionResponse> actions = approvalActionRepository
                .findByApprovalStepLeaveRequestIdOrderByCreatedAtAsc(requestId).stream()
                .map(leaveRequestMapper::toActionResponse)
                .toList();

        return leaveRequestMapper.toDetailResponse(leaveRequest, steps, actions);
    }

    @Transactional
    public void cancelRequest(AuthenticatedEmployee authenticatedEmployee, Long requestId) {
        LeaveRequestEntity leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundApplicationException("Leave request not found"));
        if (!leaveRequest.getApplicant().getId().equals(authenticatedEmployee.employeeId())) {
            throw new ForbiddenApplicationException("You are not allowed to cancel this leave request");
        }
        if (!RequestStatus.PENDING.equals(leaveRequest.getStatus())) {
            throw new BadRequestApplicationException("Only pending leave requests can be cancelled");
        }
        leaveRequest.updateStatus(RequestStatus.CANCELLED);
        leaveBalanceService.refundBalance(
                leaveRequest.getApplicant().getId(),
                leaveRequest.getType(),
                leaveRequest.getStartTime().getYear(),
                leaveRequest.getDurationMinutes()
        );
        approvalStepRepository.findByLeaveRequestIdOrderByStepOrderAsc(requestId).stream()
                .filter(step -> StepStatus.PENDING.equals(step.getStatus()))
                .forEach(step -> step.updateStatus(StepStatus.SKIPPED));
        auditLogRepository.save(AuditLogEntity.create(
                LEAVE_REQUEST_ENTITY_TYPE,
                leaveRequest.getId(),
                "CANCEL",
                leaveRequest.getApplicant(),
                buildCancelAuditDetail(leaveRequest)
        ));
    }

    // @Transactional (not readOnly) — getOrInitBalance() inside leaveBalanceService.getBalances()
    // may INSERT a new LeaveBalanceEntity on first access for a given year/type, requiring a write tx.
    @Transactional
    public List<LeaveBalanceResponse> getLeaveBalances(AuthenticatedEmployee authenticatedEmployee, int year) {
        int targetYear = (year == 0) ? java.time.Year.now().getValue() : year;
        EmployeeEntity employee = findActiveEmployee(authenticatedEmployee.employeeId(), "Employee not found");
        return leaveBalanceService.getBalances(employee.getId(), employee.getHireDate(), targetYear);
    }

    @Transactional(readOnly = true)
    public PendingRequestCountResponse getPendingRequestCount(AuthenticatedEmployee authenticatedEmployee) {
        long count = leaveRequestRepository.countByApplicantIdAndStatus(
                authenticatedEmployee.employeeId(),
                RequestStatus.PENDING
        );
        return new PendingRequestCountResponse(count);
    }

    private void validateCreateRequest(CreateLeaveRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestApplicationException("endTime must be after startTime");
        }
        if (request.deputyId() == null) {
            throw new BadRequestApplicationException("deputyId is required");
        }
    }

    private EmployeeEntity findActiveEmployee(Long employeeId, String notFoundMessage) {
        return employeeRepository.findById(employeeId)
                .filter(EmployeeEntity::getActive)
                .orElseThrow(() -> new ResourceNotFoundApplicationException(notFoundMessage));
    }

    private void validateApplicantHasNoOverlappingLeave(Long applicantId, LocalDateTime startTime, LocalDateTime endTime) {
        boolean hasConflict = leaveRequestRepository.existsOverlappingLeave(
                applicantId, startTime, endTime, List.of(RequestStatus.APPROVED, RequestStatus.PENDING)
        );
        if (hasConflict) {
            throw new BadRequestApplicationException("APPLICANT_ON_LEAVE");
        }
    }

    private EmployeeEntity resolveDeputy(Long deputyId, Long applicantId, LocalDateTime startTime, LocalDateTime endTime) {
        if (deputyId.equals(applicantId)) {
            throw new BadRequestApplicationException("deputyId cannot be the same as applicant");
        }
        EmployeeEntity deputy = findActiveEmployee(deputyId, "Deputy not found");
        boolean hasConflict = leaveRequestRepository.existsOverlappingLeave(
                deputyId, startTime, endTime, List.of(RequestStatus.APPROVED, RequestStatus.PENDING)
        );
        if (hasConflict) {
            throw new BadRequestApplicationException("DEPUTY_ON_LEAVE");
        }
        return deputy;
    }

    private String buildCreateAuditDetail(LeaveRequestEntity entity) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "status", entity.getStatus(),
                    "type", entity.getType(),
                    "durationMinutes", entity.getDurationMinutes()
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize audit detail", exception);
        }
    }

    private String buildCancelAuditDetail(LeaveRequestEntity entity) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "status", entity.getStatus(),
                    "durationMinutes", entity.getDurationMinutes()
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize audit detail", exception);
        }
    }

    private List<ApprovalStepEntity> createApprovalSteps(LeaveRequestEntity leaveRequest, EmployeeEntity applicant) {
        List<ApprovalRule> workflowRules = workflowRuleAssembler.loadActiveWorkflowRules();
        List<Long> eligibleManagerChainIds = managerChainResolver.resolveEligibleManagerChainIds(applicant);
        ApprovalFlowContext context = new ApprovalFlowContext(
                applicant.getId(),
                leaveRequest.getDeputy().getId(),
                leaveRequest.getDurationMinutes(),
                eligibleManagerChainIds,
                workflowRules
        );

        List<ApprovalFlowStep> generatedSteps = approvalFlowEngine.generateSteps(context);
        List<Long> approverIds = generatedSteps.stream().map(ApprovalFlowStep::approverId).toList();
        Map<Long, EmployeeEntity> approversById = employeeRepository.findAllById(approverIds).stream()
                .collect(java.util.stream.Collectors.toMap(EmployeeEntity::getId, employee -> employee));

        List<ApprovalStepEntity> approvalSteps = generatedSteps.stream()
                .map(step -> {
                    EmployeeEntity approver = approversById.get(step.approverId());
                    if (approver == null || !Boolean.TRUE.equals(approver.getActive())) {
                        throw new ResourceNotFoundApplicationException("Approver not found");
                    }
                    return ApprovalStepEntity.create(
                            leaveRequest,
                            step.stepOrder(),
                            approver,
                            step.stepType(),
                            StepStatus.PENDING
                    );
                })
                .toList();

        return approvalStepRepository.saveAll(approvalSteps);
    }
}
