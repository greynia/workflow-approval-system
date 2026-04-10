package com.eva.workflow.approval.infrastructure.persistence.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.ApprovalActionEntity;

public interface ApprovalActionRepository extends JpaRepository<ApprovalActionEntity, Long> {

    List<ApprovalActionEntity> findByApprovalStepIdOrderByCreatedAtAsc(Long approvalStepId);
}
