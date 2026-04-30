package com.eva.workflow.approval.infrastructure.persistence.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eva.workflow.approval.common.enums.AiReviewStatus;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.AiReviewEntity;

public interface AiReviewRepository extends JpaRepository<AiReviewEntity, Long> {

    Optional<AiReviewEntity> findByRequestId(Long requestId);

    List<AiReviewEntity> findByStatus(AiReviewStatus status);
}
