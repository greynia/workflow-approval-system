package com.eva.workflow.approval.domain.approval.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.api.exception.BusinessRuleException;
import com.eva.workflow.approval.common.enums.ApproverType;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.WorkflowDefinitionEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.WorkflowRuleEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.EmployeeRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.WorkflowDefinitionRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.WorkflowRuleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApprovalFlowEngine {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowRuleRepository workflowRuleRepository;
    private final EmployeeRepository employeeRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<com.eva.workflow.approval.domain.approval.model.ApprovalFlowStep> generateSteps(
            EmployeeEntity applicant,
            int days
    ) {
        WorkflowDefinitionEntity workflowDefinition = workflowDefinitionRepository.findFirstByActiveTrueOrderByIdAsc()
                .orElseThrow(() -> new BusinessRuleException("No active workflow definition found"));

        List<WorkflowRuleEntity> matchedRules = workflowRuleRepository
                .findByWorkflowDefinitionIdOrderByPriorityAsc(workflowDefinition.getId()).stream()
                .filter(rule -> matchesDays(rule, days))
                .toList();

        if (matchedRules.isEmpty()) {
            throw new BusinessRuleException("No workflow rule matched the leave request");
        }

        List<com.eva.workflow.approval.domain.approval.model.ApprovalFlowStep> steps = new ArrayList<>();
        Set<Long> selectedApproverIds = new LinkedHashSet<>();

        for (WorkflowRuleEntity rule : matchedRules) {
            EmployeeEntity approver = switch (rule.getApproverType()) {
                case DIRECT_MANAGER -> resolveApproverFromManagerChain(applicant, selectedApproverIds);
                // Phase 1 keeps DEPARTMENT_MANAGER on the same manager chain and relies on
                // previously selected approvers to force escalation to the next distinct manager.
                case DEPARTMENT_MANAGER -> resolveApproverFromManagerChain(applicant, selectedApproverIds);
            };

            if (!selectedApproverIds.add(approver.getId())) {
                continue;
            }

            steps.add(new com.eva.workflow.approval.domain.approval.model.ApprovalFlowStep(
                    steps.size() + 1,
                    approver.getId()
            ));
        }

        if (steps.isEmpty()) {
            throw new BusinessRuleException("No valid approval steps generated");
        }

        return steps;
    }

    private boolean matchesDays(WorkflowRuleEntity rule, int days) {
        try {
            JsonNode condition = objectMapper.readTree(rule.getConditionJson());
            if (condition.has("minDays") && days < condition.get("minDays").asInt()) {
                return false;
            }
            if (condition.has("maxDays") && days > condition.get("maxDays").asInt()) {
                return false;
            }
            return true;
        } catch (IOException exception) {
            throw new IllegalStateException("Invalid workflow rule condition JSON", exception);
        }
    }

    private EmployeeEntity resolveApproverFromManagerChain(EmployeeEntity applicant, Set<Long> excludedApproverIds) {
        EmployeeEntity current = applicant;

        while (current.getManager() != null) {
            Long managerId = current.getManager().getId();
            EmployeeEntity manager = employeeRepository.findById(managerId)
                    .orElseThrow(() -> new BusinessRuleException("Approver not found in manager chain"));

            if (Boolean.TRUE.equals(manager.getActive())
                    && !manager.getId().equals(applicant.getId())
                    && !excludedApproverIds.contains(manager.getId())) {
                return manager;
            }

            current = manager;
        }

        throw new BusinessRuleException("No valid approver found in manager chain");
    }
}
