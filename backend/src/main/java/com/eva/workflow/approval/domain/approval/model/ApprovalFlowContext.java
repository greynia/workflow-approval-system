package com.eva.workflow.approval.domain.approval.model;

import java.util.List;

public record ApprovalFlowContext(
        Long applicantId,
        Long deputyId,
        int durationMinutes,
        List<Long> eligibleManagerChainIds,
        List<ApprovalRule> workflowRules
) {

    public ApprovalFlowContext {
        eligibleManagerChainIds = List.copyOf(eligibleManagerChainIds);
        workflowRules = List.copyOf(workflowRules);
    }
}
