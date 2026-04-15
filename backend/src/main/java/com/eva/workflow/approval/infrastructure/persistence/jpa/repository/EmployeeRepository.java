package com.eva.workflow.approval.infrastructure.persistence.jpa.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eva.workflow.approval.common.enums.RequestStatus;
import com.eva.workflow.approval.common.enums.UserRole;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;

public interface EmployeeRepository extends JpaRepository<EmployeeEntity, Long> {

    interface ManagerChainRow {
        Long getEmployeeId();
        Boolean getActive();
        Boolean getCycle();
    }

    Optional<EmployeeEntity> findByEmail(String email);

    Optional<EmployeeEntity> findByEmployeeNo(String employeeNo);

    List<EmployeeEntity> findByManagerId(Long managerId);

    List<EmployeeEntity> findByActiveTrueAndIdNotOrderByNameAsc(Long employeeId);

    Optional<EmployeeEntity> findFirstByDepartmentIdAndRoleAndActiveTrue(Long departmentId, UserRole role);

    @Query("""
            select e from EmployeeEntity e
            where e.active = true
              and e.id <> :applicantId
              and not exists (
                  select 1 from LeaveRequestEntity r
                  where r.applicant.id = e.id
                    and r.status in :statuses
                    and r.startTime < :endTime
                    and r.endTime > :startTime
              )
            order by e.name asc
            """)
    List<EmployeeEntity> findAvailableDeputies(
            @Param("applicantId") Long applicantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("statuses") List<RequestStatus> statuses
    );

    @Query(value = """
            WITH RECURSIVE manager_chain AS (
                SELECT e.manager_id AS employee_id,
                       CASE
                           WHEN e.manager_id IS NULL THEN ARRAY[e.id]
                           ELSE ARRAY[e.id, e.manager_id]
                       END AS path,
                       false AS cycle,
                       1 AS depth
                FROM employees e
                WHERE e.id = :employeeId

                UNION ALL

                SELECT parent_manager.id AS employee_id,
                       mc.path || parent_manager.id,
                       parent_manager.id = ANY(mc.path) AS cycle,
                       mc.depth + 1 AS depth
                FROM manager_chain mc
                JOIN employees current_manager ON current_manager.id = mc.employee_id
                JOIN employees parent_manager ON parent_manager.id = current_manager.manager_id
                WHERE mc.employee_id IS NOT NULL
                  AND NOT mc.cycle
            )
            SELECT mc.employee_id AS employeeId,
                   e.active AS active,
                   mc.cycle AS cycle
            FROM manager_chain mc
            JOIN employees e ON e.id = mc.employee_id
            WHERE mc.employee_id IS NOT NULL
            ORDER BY mc.depth
            """, nativeQuery = true)
    List<ManagerChainRow> findManagerChainRows(@Param("employeeId") Long employeeId);
}
