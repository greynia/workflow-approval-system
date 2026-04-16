package com.eva.workflow.approval.application.request;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.eva.workflow.approval.api.dto.leave.ApprovalActionResponse;
import com.eva.workflow.approval.api.dto.leave.ApprovalStepResponse;
import com.eva.workflow.approval.api.dto.leave.LeaveRequestDetailResponse;
import com.eva.workflow.approval.api.dto.leave.LeaveRequestSummaryResponse;
import com.eva.workflow.approval.common.enums.ApprovalStepType;
import com.eva.workflow.approval.common.enums.LeaveRequestStage;
import com.eva.workflow.approval.common.enums.RequestStatus;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.ApprovalActionEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.ApprovalStepEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.LeaveRequestEntity;

@Mapper(componentModel = "spring")
public interface LeaveRequestMapper {

    default LeaveRequestSummaryResponse toSummaryResponse(
            LeaveRequestEntity entity,
            List<ApprovalStepResponse> approvalSteps
    ) {
        return new LeaveRequestSummaryResponse(
                entity.getId(),
                entity.getType(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getDurationMinutes(),
                entity.getStatus(),
                toCurrentStage(entity, approvalSteps),
                entity.getCreatedAt()
        );
    }

    default LeaveRequestDetailResponse toDetailResponse(
            LeaveRequestEntity entity,
            List<ApprovalStepResponse> approvalSteps,
            List<ApprovalActionResponse> approvalActions
    ) {
        return new LeaveRequestDetailResponse(
                entity.getId(),
                entity.getApplicant().getId(),
                entity.getApplicant().getName(),
                entity.getDeputy() == null ? null : entity.getDeputy().getId(),
                entity.getDeputy() == null ? null : entity.getDeputy().getName(),
                entity.getType(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getDurationMinutes(),
                entity.getReason(),
                entity.getStatus(),
                toCurrentStage(entity, approvalSteps),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                approvalSteps,
                approvalActions
        );
    }

    @Mapping(target = "approverId", source = "approver.id")
    @Mapping(target = "approverName", source = "approver.name")
    ApprovalStepResponse toStepResponse(ApprovalStepEntity entity);

    @Mapping(target = "actorId", source = "actor.id")
    @Mapping(target = "actorName", source = "actor.name")
    ApprovalActionResponse toActionResponse(ApprovalActionEntity entity);

    default LeaveRequestStage toCurrentStage(LeaveRequestEntity entity, List<ApprovalStepResponse> approvalSteps) {
        RequestStatus status = entity.getStatus();
        if (status == RequestStatus.APPROVED) {
            return LeaveRequestStage.APPROVED;
        }
        if (status == RequestStatus.REJECTED) {
            return LeaveRequestStage.REJECTED;
        }
        if (status == RequestStatus.CANCELLED) {
            return LeaveRequestStage.CANCELLED;
        }
        return approvalSteps.stream()
                .filter(step -> step.status() == com.eva.workflow.approval.common.enums.StepStatus.PENDING)
                .findFirst()
                .map(step -> step.stepType() == ApprovalStepType.DEPUTY
                        ? LeaveRequestStage.WAITING_DEPUTY
                        : LeaveRequestStage.WAITING_MANAGER)
                .orElseThrow(() -> new IllegalStateException(
                        "Leave request " + entity.getId() + " is PENDING but has no PENDING approval steps"));
    }
}
