package com.eva.workflow.approval.api.dto.aireview;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import com.eva.workflow.approval.common.enums.ApprovalStepType;
import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.common.enums.StepStatus;
import com.eva.workflow.approval.common.enums.UserRole;

public record AiContextResponse(
        RequestInfo request,
        ApplicantInfo applicant,
        Stats stats,
        List<ApprovalStepInfo> approvalFlow
) {

    public record RequestInfo(
            Long id,
            LeaveType leaveType,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int durationMinutes,
            String reason,
            Instant requestCreatedAt
    ) {}

    public record ApplicantInfo(
            Long id,
            UserRole role,
            String departmentName,
            long tenureDays
    ) {}

    public record Stats(
            int recentLeaveCountLast30Days
    ) {}

    public record ApprovalStepInfo(
            Integer stepOrder,
            ApprovalStepType stepType,
            StepStatus status,
            UserRole approverRole
    ) {}
}
