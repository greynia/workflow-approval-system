package com.eva.workflow.approval.infrastructure.persistence.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.LeaveRequestEntity;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequestEntity, Long> {

    Page<LeaveRequestEntity> findByApplicantIdOrderByCreatedAtDesc(Long applicantId, Pageable pageable);
}
