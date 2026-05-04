package com.eva.workflow.approval.infrastructure.persistence.jpa.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeScheduleEntity;

public interface EmployeeScheduleRepository extends JpaRepository<EmployeeScheduleEntity, Long> {

    @Query(value = """
            select schedule
            from EmployeeScheduleEntity schedule
            join fetch schedule.employee employee
            order by employee.name asc, schedule.effectiveFrom desc
            """,
            countQuery = """
                    select count(schedule)
                    from EmployeeScheduleEntity schedule
                    """)
    Page<EmployeeScheduleEntity> findAllWithEmployee(Pageable pageable);

    @Query("""
            select schedule
            from EmployeeScheduleEntity schedule
            where schedule.employee.id = :employeeId
              and schedule.effectiveFrom <= :date
              and (schedule.effectiveTo is null or schedule.effectiveTo >= :date)
            order by schedule.effectiveFrom desc
            """)
    List<EmployeeScheduleEntity> findActiveSchedules(Long employeeId, LocalDate date);
}
