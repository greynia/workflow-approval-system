package com.eva.workflow.approval.api.dto.leave;

public record LeaveBalanceResponse(
        String leaveType,
        int quotaMinutes,
        int usedMinutes,
        int remainingMinutes
) {}
