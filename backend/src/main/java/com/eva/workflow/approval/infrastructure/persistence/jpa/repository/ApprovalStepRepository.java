package com.eva.workflow.approval.infrastructure.persistence.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.eva.workflow.approval.common.enums.StepStatus;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.ApprovalStepEntity;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStepEntity, Long> {

    @Query("""
            select step
            from ApprovalStepEntity step
            join fetch step.leaveRequest leaveRequest
            join fetch leaveRequest.applicant applicant
            where step.approver.id = :approverId
              and step.status = :status
            order by step.createdAt asc
            """)
    List<ApprovalStepEntity> findPendingStepsWithRequestAndApplicant(Long approverId, StepStatus status);

    List<ApprovalStepEntity> findByLeaveRequestIdOrderByStepOrderAsc(Long leaveRequestId);

    long countByApproverIdAndStatus(Long approverId, StepStatus status);
}
