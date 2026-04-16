package com.eva.workflow.approval.application.approval;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.api.dto.approval.ApprovalDecisionRequest;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.application.exception.BadRequestApplicationException;
import com.eva.workflow.approval.application.exception.ForbiddenApplicationException;
import com.eva.workflow.approval.application.exception.ResourceNotFoundApplicationException;
import com.eva.workflow.approval.application.request.LeaveBalanceService;
import com.eva.workflow.approval.common.enums.ActionType;
import com.eva.workflow.approval.common.enums.ApprovalStepType;
import com.eva.workflow.approval.common.enums.RequestStatus;
import com.eva.workflow.approval.common.enums.StepStatus;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.ApprovalActionEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.ApprovalStepEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.AuditLogEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.LeaveRequestEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.ApprovalActionRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.ApprovalStepRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApprovalApplicationService {

    private static final String LEAVE_REQUEST_ENTITY_TYPE = "LEAVE_REQUEST";

    private final ApprovalStepRepository approvalStepRepository;
    private final ApprovalActionRepository approvalActionRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final LeaveBalanceService leaveBalanceService;

    @Transactional
    public void approveStep(Long stepId, AuthenticatedEmployee authenticatedEmployee, ApprovalDecisionRequest request) {
        processDecision(stepId, authenticatedEmployee, request, ActionType.APPROVE);
    }

    @Transactional
    public void rejectStep(Long stepId, AuthenticatedEmployee authenticatedEmployee, ApprovalDecisionRequest request) {
        processDecision(stepId, authenticatedEmployee, request, ActionType.REJECT);
    }

    private void processDecision(
            Long stepId,
            AuthenticatedEmployee authenticatedEmployee,
            ApprovalDecisionRequest request,
            ActionType actionType
    ) {
        ApprovalStepEntity step = approvalStepRepository.findWithRequestAndApproverById(stepId)
                .orElseThrow(() -> new ResourceNotFoundApplicationException("Approval step not found"));
        List<ApprovalStepEntity> allSteps = approvalStepRepository.findByLeaveRequestIdOrderByStepOrderAsc(
                step.getLeaveRequest().getId()
        );

        validateDecision(step, allSteps, authenticatedEmployee);

        LeaveRequestEntity leaveRequest = step.getLeaveRequest();

        if (actionType == ActionType.APPROVE) {
            step.updateStatus(StepStatus.APPROVED);
            boolean allApproved = allSteps.stream()
                    .allMatch(existingStep -> StepStatus.APPROVED.equals(existingStep.getStatus()));
            if (allApproved) {
                leaveRequest.updateStatus(RequestStatus.APPROVED);
            }
        } else {
            step.updateStatus(StepStatus.REJECTED);
            leaveRequest.updateStatus(RequestStatus.REJECTED);
            allSteps.stream()
                    .filter(existingStep -> existingStep.getStepOrder() > step.getStepOrder())
                    .filter(existingStep -> StepStatus.PENDING.equals(existingStep.getStatus()))
                    .forEach(existingStep -> existingStep.updateStatus(StepStatus.SKIPPED));
            leaveBalanceService.refundBalance(
                    leaveRequest.getApplicant().getId(),
                    leaveRequest.getType(),
                    leaveRequest.getStartTime().getYear(),
                    leaveRequest.getDurationMinutes()
            );
        }

        approvalActionRepository.save(ApprovalActionEntity.create(
                step,
                step.getApprover(),
                actionType,
                request.comment()
        ));

        auditLogRepository.save(AuditLogEntity.create(
                LEAVE_REQUEST_ENTITY_TYPE,
                leaveRequest.getId(),
                actionType.name(),
                step.getApprover(),
                buildAuditDetail(step, leaveRequest, request, actionType)
        ));
    }

    private void validateDecision(
            ApprovalStepEntity step,
            List<ApprovalStepEntity> allSteps,
            AuthenticatedEmployee authenticatedEmployee
    ) {
        if (!step.getApprover().getId().equals(authenticatedEmployee.employeeId())) {
            throw new ForbiddenApplicationException("You are not allowed to process this approval step");
        }
        if (!StepStatus.PENDING.equals(step.getStatus())) {
            throw new BadRequestApplicationException("Approval step has already been processed");
        }
        boolean previousStepsIncomplete = allSteps.stream()
                .filter(existingStep -> existingStep.getStepOrder() < step.getStepOrder())
                .anyMatch(existingStep -> !StepStatus.APPROVED.equals(existingStep.getStatus()));
        if (previousStepsIncomplete) {
            throw new BadRequestApplicationException("Previous approval step has not been completed");
        }
    }

    private String buildAuditDetail(
            ApprovalStepEntity step,
            LeaveRequestEntity leaveRequest,
            ApprovalDecisionRequest request,
            ActionType actionType
    ) {
        try {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("stepId", step.getId());
            detail.put("stepType", step.getStepType());
            detail.put("requestStatus", leaveRequest.getStatus());
            detail.put("stepStatus", step.getStatus());
            detail.put("actionType", actionType);
            detail.put("comment", request.comment());
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize audit detail", exception);
        }
    }
}
