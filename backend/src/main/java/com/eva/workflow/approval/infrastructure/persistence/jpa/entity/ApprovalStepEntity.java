package com.eva.workflow.approval.infrastructure.persistence.jpa.entity;

import java.time.LocalDateTime;

import com.eva.workflow.approval.common.enums.ApprovalStepType;
import com.eva.workflow.approval.common.enums.StepStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "approval_steps")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalStepEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_request_id", nullable = false)
    private LeaveRequestEntity leaveRequest;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", nullable = false)
    private EmployeeEntity approver;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 20)
    private ApprovalStepType stepType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StepStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static ApprovalStepEntity create(
            LeaveRequestEntity leaveRequest,
            Integer stepOrder,
            EmployeeEntity approver,
            ApprovalStepType stepType,
            StepStatus status
    ) {
        ApprovalStepEntity entity = new ApprovalStepEntity();
        entity.leaveRequest = leaveRequest;
        entity.stepOrder = stepOrder;
        entity.approver = approver;
        entity.stepType = stepType;
        entity.status = status;
        return entity;
    }

    public void updateStatus(StepStatus status) {
        this.status = status;
    }

}
