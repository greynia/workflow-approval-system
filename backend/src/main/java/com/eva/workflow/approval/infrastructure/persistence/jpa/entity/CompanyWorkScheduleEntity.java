package com.eva.workflow.approval.infrastructure.persistence.jpa.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "company_work_schedules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyWorkScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_start", nullable = false)
    private LocalTime workStart;

    @Column(name = "work_end", nullable = false)
    private LocalTime workEnd;

    @Column(name = "lunch_start", nullable = false)
    private LocalTime lunchStart;

    @Column(name = "lunch_end", nullable = false)
    private LocalTime lunchEnd;

    @Column(name = "work_days", nullable = false)
    private String workDays;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
