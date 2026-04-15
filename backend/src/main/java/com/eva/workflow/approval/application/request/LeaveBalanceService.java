package com.eva.workflow.approval.application.request;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.api.dto.leave.LeaveBalanceResponse;
import com.eva.workflow.approval.application.exception.BadRequestApplicationException;
import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.LeaveBalanceEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.LeaveBalanceRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class LeaveBalanceService {

    private static final int WORK_MINUTES_PER_DAY = 480;
    private static final int SICK_LEAVE_DAYS      = 30;
    private static final int PERSONAL_LEAVE_DAYS  = 7;

    private final LeaveBalanceRepository leaveBalanceRepository;

    /**
     * 依勞基法年資規則計算特休天數。
     *
     * 規則（勞基法第38條）：
     *   未滿 6 個月         → 0 天
     *   6 個月以上未滿 1 年  → 3 天
     *   1 年以上未滿 2 年   → 7 天
     *   2 年以上未滿 3 年   → 10 天
     *   3 年以上未滿 5 年   → 14 天
     *   5 年以上未滿 10 年  → 15 天
     *   10 年以上           → 15 + (滿幾年 - 10)，最多 30 天
     *
     * 計算基準：取「year 年內最晚一次工作週年紀念日」的滿幾年。
     * 若 hireDate 在 year 年之後（尚未滿 6 個月）則回傳 0。
     *
     * @param hireDate 入職日期
     * @param year     目標年度（例如 2026）
     * @return 特休天數
     */
    int calculateAnnualLeaveQuotaDays(LocalDate hireDate, int year) {
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        if (hireDate.isAfter(yearEnd)) {
            return 0;
        }

        long completedMonthsByYearEnd = ChronoUnit.MONTHS.between(hireDate, yearEnd.plusDays(1));
        if (completedMonthsByYearEnd < 6) {
            return 0;
        }

        LocalDate anniversary = latestAnniversaryInYear(hireDate, year);
        long completedYears = ChronoUnit.YEARS.between(hireDate, anniversary);

        if (completedYears < 1) {
            return 3;
        }
        if (completedYears < 2) {
            return 7;
        }
        if (completedYears < 3) {
            return 10;
        }
        if (completedYears < 5) {
            return 14;
        }
        if (completedYears < 10) {
            return 15;
        }

        return Math.min(15 + (int) (completedYears - 10), 30);
    }

    private LocalDate latestAnniversaryInYear(LocalDate hireDate, int year) {
        int day = Math.min(hireDate.getDayOfMonth(), LocalDate.of(year, hireDate.getMonth(), 1).lengthOfMonth());
        return LocalDate.of(year, hireDate.getMonth(), day);
    }

    /**
     * 預扣假期餘額（送出申請時呼叫，PENDING 即扣）。
     * OTHER 類別不追蹤，直接跳過。
     */
    public void deductBalance(Long employeeId, LocalDate hireDate, LeaveType leaveType, int year, int minutes) {
        if (leaveType == LeaveType.OTHER) return;
        LeaveBalanceEntity balance = getOrInitBalance(employeeId, hireDate, leaveType, year);
        if (balance.getRemainingMinutes() < minutes) {
            throw new BadRequestApplicationException("INSUFFICIENT_LEAVE_BALANCE");
        }
        balance.addUsed(minutes);
    }

    /**
     * 退回假期餘額（申請被退回或取消時呼叫）。
     * 若找不到餘額紀錄（理論上不應發生）則靜默忽略。
     */
    public void refundBalance(Long employeeId, LeaveType leaveType, int year, int minutes) {
        if (leaveType == LeaveType.OTHER) return;
        leaveBalanceRepository
                .findByEmployeeIdAndYearAndLeaveType(employeeId, year, leaveType)
                .ifPresent(balance -> balance.subtractUsed(minutes));
    }

    /**
     * 查詢指定年度的所有假別餘額。若某假別紀錄不存在則 lazy init。
     */
    public List<LeaveBalanceResponse> getBalances(Long employeeId, LocalDate hireDate, int year) {
        return List.of(LeaveType.ANNUAL, LeaveType.SICK, LeaveType.PERSONAL).stream()
                .map(type -> {
                    LeaveBalanceEntity balance = getOrInitBalance(employeeId, hireDate, type, year);
                    return new LeaveBalanceResponse(
                            type.name(),
                            balance.getQuotaMinutes(),
                            balance.getUsedMinutes(),
                            balance.getRemainingMinutes()
                    );
                })
                .toList();
    }

    private LeaveBalanceEntity getOrInitBalance(Long employeeId, LocalDate hireDate, LeaveType leaveType, int year) {
        return leaveBalanceRepository
                .findByEmployeeIdAndYearAndLeaveType(employeeId, year, leaveType)
                .orElseGet(() -> {
                    int quota = calculateQuotaMinutes(leaveType, hireDate, year);
                    return leaveBalanceRepository.save(
                            LeaveBalanceEntity.create(employeeId, year, leaveType, quota));
                });
    }

    private int calculateQuotaMinutes(LeaveType leaveType, LocalDate hireDate, int year) {
        return switch (leaveType) {
            case ANNUAL   -> calculateAnnualLeaveQuotaDays(hireDate, year) * WORK_MINUTES_PER_DAY;
            case SICK     -> SICK_LEAVE_DAYS * WORK_MINUTES_PER_DAY;
            case PERSONAL -> PERSONAL_LEAVE_DAYS * WORK_MINUTES_PER_DAY;
            case OTHER    -> 0;
        };
    }
}
