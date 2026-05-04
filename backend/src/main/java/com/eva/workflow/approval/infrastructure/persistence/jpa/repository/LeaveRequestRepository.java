package com.eva.workflow.approval.infrastructure.persistence.jpa.repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eva.workflow.approval.common.enums.RequestStatus;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.LeaveRequestEntity;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequestEntity, Long> {

    Page<LeaveRequestEntity> findByApplicantIdOrderByCreatedAtDesc(Long applicantId, Pageable pageable);

    long countByApplicantIdAndStatus(Long applicantId, RequestStatus status);

    @Query("""
            select case when count(r) > 0 then true else false end
            from LeaveRequestEntity r
            where r.applicant.id = :employeeId
            and r.status in :statuses
            and r.startTime < :endTime
            and r.endTime > :startTime
            """)
    boolean existsOverlappingLeave(
            @Param("employeeId") Long employeeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("statuses") List<RequestStatus> statuses
    );

    @Query("""
            select count(r) from LeaveRequestEntity r
            where r.applicant.id = :applicantId
            and r.id <> :excludedRequestId
            and r.status in :statuses
            and r.createdAt >= :since
            """)
    long countRecentByApplicantIdExcludingRequest(
            @Param("applicantId") Long applicantId,
            @Param("excludedRequestId") Long excludedRequestId,
            @Param("statuses") List<RequestStatus> statuses,
            @Param("since") Instant since
    );
}
