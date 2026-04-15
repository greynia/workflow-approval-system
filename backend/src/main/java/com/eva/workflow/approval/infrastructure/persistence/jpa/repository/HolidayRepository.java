package com.eva.workflow.approval.infrastructure.persistence.jpa.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.HolidayEntity;

public interface HolidayRepository extends JpaRepository<HolidayEntity, Long> {

    List<HolidayEntity> findByDateBetween(LocalDate startDate, LocalDate endDate);
}
