package com.eva.workflow.approval.domain.approval.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.eva.workflow.approval.common.enums.ApprovalStepType;
import com.eva.workflow.approval.domain.approval.exception.BusinessRuleException;
import com.eva.workflow.approval.domain.approval.model.ApprovalFlowContext;
import com.eva.workflow.approval.domain.approval.model.ApprovalFlowStep;
import com.eva.workflow.approval.domain.approval.model.ApprovalRule;

@Service
public class ApprovalFlowEngine {

    public List<ApprovalFlowStep> generateSteps(ApprovalFlowContext context) {
        List<ApprovalRule> matchedRules = context.workflowRules().stream()
                .filter(rule -> rule.matches(context.durationMinutes()))
                .sorted(Comparator.comparingInt(ApprovalRule::stepOrder))
                .toList();

        if (matchedRules.isEmpty()) {
            throw new BusinessRuleException("No workflow rule matched the leave request");
        }

        List<ApprovalFlowStep> steps = new ArrayList<>();
        Set<Long> selectedApproverIds = new LinkedHashSet<>();

        if (context.deputyId() == null || context.deputyId().equals(context.applicantId())) {
            throw new BusinessRuleException("No valid deputy selected");
        }
        selectedApproverIds.add(context.deputyId());
        steps.add(new ApprovalFlowStep(1, context.deputyId(), ApprovalStepType.DEPUTY));

        for (ApprovalRule rule : matchedRules) {
            Long approverId = switch (rule.approverType()) {
                case DIRECT_MANAGER -> resolveDirectManager(context, selectedApproverIds);
                case DEPARTMENT_MANAGER -> resolveDepartmentManager(context, selectedApproverIds);
            };

            selectedApproverIds.add(approverId);
            steps.add(new ApprovalFlowStep(steps.size() + 1, approverId, ApprovalStepType.MANAGER));
        }

        return steps;
    }

    private Long resolveDirectManager(ApprovalFlowContext context, Set<Long> excludedApproverIds) {
        return resolveNextManagerInChain(context, excludedApproverIds);
    }

    // Current Phase 1 semantics keep department manager on the same manager chain
    // and escalate to the next distinct approver that has not already been selected.
    private Long resolveDepartmentManager(ApprovalFlowContext context, Set<Long> excludedApproverIds) {
        return resolveNextManagerInChain(context, excludedApproverIds);
    }

    private Long resolveNextManagerInChain(ApprovalFlowContext context, Set<Long> excludedApproverIds) {
        return context.eligibleManagerChainIds().stream()
                .filter(approverId -> !approverId.equals(context.applicantId()))
                .filter(approverId -> !excludedApproverIds.contains(approverId))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("No valid approver found in manager chain"));
    }
}
