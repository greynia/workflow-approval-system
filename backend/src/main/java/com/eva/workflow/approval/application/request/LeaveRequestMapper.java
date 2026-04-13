package com.eva.workflow.approval.application.request;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.eva.workflow.approval.api.dto.leave.ApprovalActionResponse;
import com.eva.workflow.approval.api.dto.leave.ApprovalStepResponse;
import com.eva.workflow.approval.api.dto.leave.LeaveRequestDetailResponse;
import com.eva.workflow.approval.api.dto.leave.LeaveRequestSummaryResponse;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.ApprovalActionEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.ApprovalStepEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.LeaveRequestEntity;

@Mapper(componentModel = "spring")
public interface LeaveRequestMapper {

    LeaveRequestSummaryResponse toSummaryResponse(LeaveRequestEntity entity);

    @Mapping(target = "applicantId", source = "entity.applicant.id")
    @Mapping(target = "applicantName", source = "entity.applicant.name")
    @Mapping(target = "deputyId", source = "entity.deputy.id")
    @Mapping(target = "deputyName", source = "entity.deputy.name")
    @Mapping(target = "approvalSteps", source = "approvalSteps")
    @Mapping(target = "approvalActions", source = "approvalActions")
    LeaveRequestDetailResponse toDetailResponse(
            LeaveRequestEntity entity,
            List<ApprovalStepResponse> approvalSteps,
            List<ApprovalActionResponse> approvalActions
    );

    @Mapping(target = "approverId", source = "approver.id")
    @Mapping(target = "approverName", source = "approver.name")
    ApprovalStepResponse toStepResponse(ApprovalStepEntity entity);

    @Mapping(target = "actorId", source = "actor.id")
    @Mapping(target = "actorName", source = "actor.name")
    ApprovalActionResponse toActionResponse(ApprovalActionEntity entity);
}
