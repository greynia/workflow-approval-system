package com.eva.workflow.approval.api.dto.admin;

import java.time.Instant;

public record AdminLeaveBalanceResponse(
        Long id,
        Long employeeId,
        String employeeName,
        String leaveType,
        int quotaMinutes,
        int usedMinutes,
        int remainingMinutes,
        Instant updatedAt
) {}
