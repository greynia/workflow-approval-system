package com.eva.workflow.approval.infrastructure.persistence.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.LeaveBalanceEntity;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalanceEntity, Long> {

    Optional<LeaveBalanceEntity> findByEmployeeIdAndYearAndLeaveType(
            Long employeeId, Integer year, LeaveType leaveType);

    List<LeaveBalanceEntity> findByEmployeeIdAndYearOrderByLeaveType(
            Long employeeId, Integer year);

    @Query(value = "SELECT b FROM LeaveBalanceEntity b WHERE b.year = :year ORDER BY b.employeeId, b.leaveType",
           countQuery = "SELECT COUNT(b) FROM LeaveBalanceEntity b WHERE b.year = :year")
    Page<LeaveBalanceEntity> findAllByYearOrderByEmployeeIdAndLeaveType(@Param("year") int year, Pageable pageable);

    List<LeaveBalanceEntity> findAllByEmployeeIdAndYear(Long employeeId, int year);

    Page<LeaveBalanceEntity> findAllByEmployeeIdAndYearOrderByLeaveType(Long employeeId, int year, Pageable pageable);

    @Modifying
    @Query(value = """
            INSERT INTO employee_leave_balances (employee_id, year, leave_type, quota_minutes, used_minutes)
            VALUES (:employeeId, :year, :leaveType, :quotaMinutes, 0)
            ON CONFLICT (employee_id, year, leave_type) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("employeeId") Long employeeId, @Param("year") int year,
                       @Param("leaveType") String leaveType, @Param("quotaMinutes") int quotaMinutes);
}
