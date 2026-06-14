package com.eva.workflow.approval.infrastructure.persistence.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.WorkflowRuleEntity;

public interface WorkflowRuleRepository extends JpaRepository<WorkflowRuleEntity, Long> {

    List<WorkflowRuleEntity> findByWorkflowDefinitionIdOrderByPriorityAsc(Long workflowDefinitionId);
}
