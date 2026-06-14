package com.eva.workflow.approval.infrastructure.persistence.jpa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.WorkflowDefinitionEntity;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinitionEntity, Long> {

    Optional<WorkflowDefinitionEntity> findFirstByActiveTrueOrderByIdAsc();
}
