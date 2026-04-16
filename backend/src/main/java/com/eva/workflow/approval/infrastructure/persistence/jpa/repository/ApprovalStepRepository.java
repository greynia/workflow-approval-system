package com.eva.workflow.approval.infrastructure.persistence.jpa.repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eva.workflow.approval.common.enums.StepStatus;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.ApprovalStepEntity;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStepEntity, Long> {

    @Query("""
            select step
            from ApprovalStepEntity step
            join fetch step.leaveRequest leaveRequest
            join fetch leaveRequest.applicant applicant
            join fetch step.approver approver
            where step.approver.id = :approverId
              and step.status = :status
              and not exists (
                  select 1
                  from ApprovalStepEntity previousStep
                  where previousStep.leaveRequest.id = step.leaveRequest.id
                    and previousStep.stepOrder < step.stepOrder
                    and previousStep.status <> :approvedStatus
              )
            order by step.createdAt asc
            """)
    List<ApprovalStepEntity> findPendingStepsWithRequestAndApplicant(Long approverId, StepStatus status, StepStatus approvedStatus);

    @Query("""
            select step
            from ApprovalStepEntity step
            join fetch step.leaveRequest leaveRequest
            join fetch leaveRequest.applicant applicant
            join fetch step.approver approver
            where step.id = :stepId
            """)
    java.util.Optional<ApprovalStepEntity> findWithRequestAndApproverById(Long stepId);

    List<ApprovalStepEntity> findByLeaveRequestIdOrderByStepOrderAsc(Long leaveRequestId);

    List<ApprovalStepEntity> findByLeaveRequestId(Long leaveRequestId);

    @Query("""
            select s from ApprovalStepEntity s
            join fetch s.approver
            where s.leaveRequest.id in :requestIds
            order by s.stepOrder asc
            """)
    List<ApprovalStepEntity> findByLeaveRequestIdInOrderByStepOrderAsc(@Param("requestIds") List<Long> requestIds);

    default Map<Long, List<ApprovalStepEntity>> findGroupedByLeaveRequestIds(List<Long> requestIds) {
        if (requestIds.isEmpty()) {
            return Map.of();
        }
        return findByLeaveRequestIdInOrderByStepOrderAsc(requestIds).stream()
                .collect(Collectors.groupingBy(step -> step.getLeaveRequest().getId()));
    }

    boolean existsByLeaveRequestIdAndApproverId(Long leaveRequestId, Long approverId);

    @Query("""
            select count(step)
            from ApprovalStepEntity step
            where step.approver.id = :approverId
              and step.status = :status
              and not exists (
                  select 1
                  from ApprovalStepEntity previousStep
                  where previousStep.leaveRequest.id = step.leaveRequest.id
                    and previousStep.stepOrder < step.stepOrder
                    and previousStep.status <> :approvedStatus
              )
            """)
    long countActionablePendingStepsByApproverId(Long approverId, StepStatus status, StepStatus approvedStatus);
}
