package com.eva.workflow.approval.domain.aireview.model;

import java.time.Instant;
import java.time.LocalDateTime;

import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.common.enums.UserRole;

public record ReviewSnapshot(
        Long requestId,
        LeaveType leaveType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        int durationMinutes,
        String reason,
        Instant requestCreatedAt,

        Long applicantId,
        long applicantTenureDays,
        UserRole applicantRole,
        String departmentName,

        int recentLeaveCountLast30Days,
        int approvalStepCount
) {
}
