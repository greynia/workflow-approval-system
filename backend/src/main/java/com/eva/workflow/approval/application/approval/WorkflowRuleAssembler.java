package com.eva.workflow.approval.application.approval;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.application.exception.ApplicationConfigurationException;
import com.eva.workflow.approval.domain.approval.model.ApprovalRule;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.WorkflowRuleEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.WorkflowDefinitionRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.WorkflowRuleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkflowRuleAssembler {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowRuleRepository workflowRuleRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ApprovalRule> loadActiveWorkflowRules() {
        Long workflowDefinitionId = workflowDefinitionRepository.findFirstByActiveTrueOrderByIdAsc()
                .orElseThrow(() -> new ApplicationConfigurationException("No active workflow definition found"))
                .getId();

        return workflowRuleRepository.findByWorkflowDefinitionIdOrderByPriorityAsc(workflowDefinitionId).stream()
                .map(this::toApprovalRule)
                .toList();
    }

    private ApprovalRule toApprovalRule(WorkflowRuleEntity entity) {
        JsonNode condition = parseCondition(entity.getConditionJson());
        return new ApprovalRule(
                condition.has("minMinutes") ? condition.get("minMinutes").asInt() : null,
                condition.has("maxMinutes") ? condition.get("maxMinutes").asInt() : null,
                entity.getApproverType(),
                entity.getPriority()
        );
    }

    private JsonNode parseCondition(String conditionJson) {
        try {
            return objectMapper.readTree(conditionJson);
        } catch (IOException exception) {
            throw new ApplicationConfigurationException("Invalid workflow rule condition JSON");
        }
    }
}
