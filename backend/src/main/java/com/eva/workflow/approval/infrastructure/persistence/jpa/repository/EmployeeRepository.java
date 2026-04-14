package com.eva.workflow.approval.infrastructure.persistence.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eva.workflow.approval.common.enums.UserRole;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;

public interface EmployeeRepository extends JpaRepository<EmployeeEntity, Long> {

    Optional<EmployeeEntity> findByEmail(String email);

    Optional<EmployeeEntity> findByEmployeeNo(String employeeNo);

    List<EmployeeEntity> findByManagerId(Long managerId);

    List<EmployeeEntity> findByActiveTrueAndIdNotOrderByNameAsc(Long employeeId);

    Optional<EmployeeEntity> findFirstByDepartmentIdAndRoleAndActiveTrue(Long departmentId, UserRole role);
}
