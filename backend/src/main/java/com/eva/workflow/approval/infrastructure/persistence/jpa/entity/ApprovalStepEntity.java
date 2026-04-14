package com.eva.workflow.approval.infrastructure.persistence.jpa.entity;

import java.time.LocalDateTime;

import com.eva.workflow.approval.common.enums.StepStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "approval_steps")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalStepEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_request_id", nullable = false)
    private LeaveRequestEntity leaveRequest;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", nullable = false)
    private EmployeeEntity approver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StepStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static ApprovalStepEntity create(
            LeaveRequestEntity leaveRequest,
            Integer stepOrder,
            EmployeeEntity approver,
            StepStatus status
    ) {
        ApprovalStepEntity entity = new ApprovalStepEntity();
        LocalDateTime now = LocalDateTime.now();
        entity.leaveRequest = leaveRequest;
        entity.stepOrder = stepOrder;
        entity.approver = approver;
        entity.status = status;
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
