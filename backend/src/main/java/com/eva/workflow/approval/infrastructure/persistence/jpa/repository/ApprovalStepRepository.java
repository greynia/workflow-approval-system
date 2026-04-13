package com.eva.workflow.approval.infrastructure.persistence.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eva.workflow.approval.common.enums.StepStatus;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.ApprovalStepEntity;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStepEntity, Long> {

    List<ApprovalStepEntity> findByApproverIdAndStatusOrderByCreatedAtAsc(Long approverId, StepStatus status);

    List<ApprovalStepEntity> findByLeaveRequestIdOrderByStepOrderAsc(Long leaveRequestId);

    long countByApproverIdAndStatus(Long approverId, StepStatus status);
}
