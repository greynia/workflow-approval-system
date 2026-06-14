package com.eva.workflow.approval.application.request.event;

public record LeaveRequestCreatedEvent(
        Long leaveRequestId,
        Long applicantId,
        String locale
) {
}
