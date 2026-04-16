package com.eva.workflow.approval.infrastructure.persistence.jpa.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.CompanyWorkScheduleEntity;

public interface CompanyWorkScheduleRepository extends JpaRepository<CompanyWorkScheduleEntity, Long> {

    Optional<CompanyWorkScheduleEntity> findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDate date);
}
