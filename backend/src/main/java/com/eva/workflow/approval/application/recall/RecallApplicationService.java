package com.eva.workflow.approval.application.recall;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.api.dto.recall.PendingRecallCountResponse;
import com.eva.workflow.approval.api.dto.recall.PendingRecallResponse;
import com.eva.workflow.approval.application.audit.AuditLogService;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.application.exception.BadRequestApplicationException;
import com.eva.workflow.approval.application.exception.ForbiddenApplicationException;
import com.eva.workflow.approval.application.exception.ResourceNotFoundApplicationException;
import com.eva.workflow.approval.application.request.LeaveBalanceService;
import com.eva.workflow.approval.common.AuditEntityTypes;
import com.eva.workflow.approval.common.Permission;
import com.eva.workflow.approval.common.enums.ApprovalStepType;
import com.eva.workflow.approval.common.enums.RequestStatus;
import com.eva.workflow.approval.common.enums.StepStatus;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.ApprovalStepEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.LeaveRequestEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.ApprovalStepRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecallApplicationService {

    private final ApprovalStepRepository approvalStepRepository;
    private final AuditLogService auditLogService;
    private final LeaveBalanceService leaveBalanceService;

    @Transactional(readOnly = true)
    public List<PendingRecallResponse> getPendingRecalls(AuthenticatedEmployee manager) {
        assertRecallManagePermission(manager);
        return approvalStepRepository
                .findByApproverIdAndStepTypeAndStatus(manager.employeeId(), ApprovalStepType.RECALL, StepStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PendingRecallCountResponse getPendingRecallCount(AuthenticatedEmployee manager) {
        assertRecallManagePermission(manager);
        long count = approvalStepRepository.countByApproverIdAndStepTypeAndStatus(
                manager.employeeId(), ApprovalStepType.RECALL, StepStatus.PENDING);
        return new PendingRecallCountResponse(count);
    }

    @Transactional
    public void approveRecall(Long stepId, AuthenticatedEmployee manager) {
        assertRecallManagePermission(manager);
        ApprovalStepEntity step = loadAndValidateRecallStep(stepId, manager.employeeId());
        LeaveRequestEntity request = step.getLeaveRequest();

        step.updateStatus(StepStatus.APPROVED);
        request.updateStatus(RequestStatus.CANCELLED);

        leaveBalanceService.refundBalance(
                request.getApplicant().getId(),
                request.getType(),
                request.getStartTime().getYear(),
                request.getDurationMinutes()
        );

        auditLogService.log(
                AuditEntityTypes.LEAVE_REQUEST,
                request.getId(),
                "RECALL_APPROVED",
                step.getApprover(),
                Map.of("stepId", stepId, "durationMinutes", request.getDurationMinutes())
        );
    }

    @Transactional
    public void rejectRecall(Long stepId, AuthenticatedEmployee manager, String comment) {
        assertRecallManagePermission(manager);
        ApprovalStepEntity step = loadAndValidateRecallStep(stepId, manager.employeeId());
        LeaveRequestEntity request = step.getLeaveRequest();

        step.updateStatus(StepStatus.REJECTED);
        request.updateStatus(RequestStatus.APPROVED);

        auditLogService.log(
                AuditEntityTypes.LEAVE_REQUEST,
                request.getId(),
                "RECALL_REJECTED",
                step.getApprover(),
                Map.of("stepId", stepId, "comment", comment != null ? comment : "")
        );
    }

    private ApprovalStepEntity loadAndValidateRecallStep(Long stepId, Long managerId) {
        ApprovalStepEntity step = approvalStepRepository.findWithRequestAndApproverById(stepId)
                .orElseThrow(() -> new ResourceNotFoundApplicationException("Recall step not found"));
        if (!step.getApprover().getId().equals(managerId)) {
            throw new ForbiddenApplicationException("You are not the approver of this recall step");
        }
        if (step.getStepType() != ApprovalStepType.RECALL) {
            throw new BadRequestApplicationException("Step is not a recall step");
        }
        if (step.getStatus() != StepStatus.PENDING) {
            throw new BadRequestApplicationException("Recall step is not pending");
        }
        return step;
    }

    private void assertRecallManagePermission(AuthenticatedEmployee employee) {
        if (!employee.permissions().contains(Permission.RECALL_MANAGE)) {
            throw new ForbiddenApplicationException("You do not have permission to manage recalls");
        }
    }

    private PendingRecallResponse toResponse(ApprovalStepEntity step) {
        LeaveRequestEntity request = step.getLeaveRequest();
        return new PendingRecallResponse(
                step.getId(),
                request.getId(),
                request.getApplicant().getId(),
                request.getApplicant().getName(),
                request.getType(),
                request.getDurationMinutes(),
                request.getStartTime(),
                request.getEndTime(),
                request.getRecallReason()
        );
    }
}
