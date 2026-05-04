package com.eva.workflow.approval.infrastructure.persistence.jpa.entity;

import java.time.Instant;

import com.eva.workflow.approval.common.enums.LeaveType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "employee_leave_balances")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeaveBalanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 20)
    private LeaveType leaveType;

    @Column(name = "quota_minutes", nullable = false)
    private Integer quotaMinutes;

    @Column(name = "used_minutes", nullable = false)
    private Integer usedMinutes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static LeaveBalanceEntity create(Long employeeId, int year, LeaveType leaveType, int quotaMinutes) {
        LeaveBalanceEntity entity = new LeaveBalanceEntity();
        entity.employeeId = employeeId;
        entity.year = year;
        entity.leaveType = leaveType;
        entity.quotaMinutes = quotaMinutes;
        entity.usedMinutes = 0;
        return entity;
    }

    public int getRemainingMinutes() {
        return this.quotaMinutes - this.usedMinutes;
    }

    public void addUsed(int minutes) {
        this.usedMinutes += minutes;
    }

    public void subtractUsed(int minutes) {
        this.usedMinutes = Math.max(0, this.usedMinutes - minutes);
    }

}
