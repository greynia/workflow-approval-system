package com.eva.workflow.approval.infrastructure.persistence.jpa.entity;

import java.time.LocalDateTime;

import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.common.enums.RequestStatus;

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
@Table(name = "leave_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeaveRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private EmployeeEntity applicant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deputy_id")
    private EmployeeEntity deputy;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private LeaveType type;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static LeaveRequestEntity create(
            EmployeeEntity applicant,
            EmployeeEntity deputy,
            LeaveType type,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer durationMinutes,
            String reason,
            RequestStatus status
    ) {
        LeaveRequestEntity entity = new LeaveRequestEntity();
        LocalDateTime now = LocalDateTime.now();
        entity.applicant = applicant;
        entity.deputy = deputy;
        entity.type = type;
        entity.startTime = startTime;
        entity.endTime = endTime;
        entity.durationMinutes = durationMinutes;
        entity.reason = reason;
        entity.status = status;
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    public void updateStatus(RequestStatus status) {
        this.status = status;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
