package com.eva.workflow.approval.infrastructure.persistence.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.DepartmentEntity;

public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Long> {

    List<DepartmentEntity> findByParentDepartmentId(Long parentDepartmentId);
}
