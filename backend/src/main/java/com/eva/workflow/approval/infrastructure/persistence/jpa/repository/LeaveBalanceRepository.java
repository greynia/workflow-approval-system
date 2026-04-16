package com.eva.workflow.approval.infrastructure.persistence.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.LeaveBalanceEntity;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalanceEntity, Long> {

    Optional<LeaveBalanceEntity> findByEmployeeIdAndYearAndLeaveType(
            Long employeeId, Integer year, LeaveType leaveType);

    List<LeaveBalanceEntity> findByEmployeeIdAndYearOrderByLeaveType(
            Long employeeId, Integer year);
}
