package com.eva.workflow.approval.application.approval;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.api.dto.approval.PendingApprovalCountResponse;
import com.eva.workflow.approval.api.dto.approval.PendingApprovalResponse;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.common.enums.StepStatus;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.ApprovalStepEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.ApprovalStepRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApprovalQueryService {

    private final ApprovalStepRepository approvalStepRepository;

    @Transactional(readOnly = true)
    public List<PendingApprovalResponse> getPendingApprovals(AuthenticatedEmployee authenticatedEmployee) {
        return approvalStepRepository.findPendingStepsWithRequestAndApplicant(
                        authenticatedEmployee.employeeId(),
                        StepStatus.PENDING,
                        StepStatus.APPROVED
                ).stream()
                .map(this::toPendingApprovalResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PendingApprovalCountResponse getPendingApprovalCount(AuthenticatedEmployee authenticatedEmployee) {
        long count = approvalStepRepository.countActionablePendingStepsByApproverId(
                authenticatedEmployee.employeeId(),
                StepStatus.PENDING,
                StepStatus.APPROVED
        );
        return new PendingApprovalCountResponse(count);
    }

    private PendingApprovalResponse toPendingApprovalResponse(ApprovalStepEntity step) {
        return new PendingApprovalResponse(
                step.getId(),
                step.getStepType(),
                step.getLeaveRequest().getId(),
                step.getLeaveRequest().getApplicant().getId(),
                step.getLeaveRequest().getApplicant().getName(),
                step.getLeaveRequest().getType(),
                step.getLeaveRequest().getDurationMinutes(),
                step.getLeaveRequest().getStartTime(),
                step.getLeaveRequest().getEndTime(),
                step.getStatus(),
                step.getCreatedAt()
        );
    }
}
