package com.eva.workflow.approval.application.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.LeaveBalanceRepository;

@ExtendWith(MockitoExtension.class)
class LeaveBalanceServiceTest {

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    private LeaveBalanceService leaveBalanceService;

    @BeforeEach
    void setUp() {
        leaveBalanceService = new LeaveBalanceService(leaveBalanceRepository);
    }

    @Test
    void annualLeaveIsZeroWhenEmploymentIsLessThanSixMonthsByYearEnd() {
        assertThat(leaveBalanceService.calculateAnnualLeaveQuotaDays(LocalDate.of(2026, 7, 2), 2026))
                .isZero();
    }

    @Test
    void annualLeaveIsThreeDaysWhenEmploymentReachesSixMonthsButNotOneYear() {
        assertThat(leaveBalanceService.calculateAnnualLeaveQuotaDays(LocalDate.of(2026, 7, 1), 2026))
                .isEqualTo(3);
    }

    @Test
    void annualLeaveIsSevenDaysWhenCompletedYearsIsOne() {
        assertThat(leaveBalanceService.calculateAnnualLeaveQuotaDays(LocalDate.of(2025, 4, 1), 2026))
                .isEqualTo(7);
    }

    @Test
    void annualLeaveIsFourteenDaysWhenCompletedYearsIsBetweenThreeAndFour() {
        assertThat(leaveBalanceService.calculateAnnualLeaveQuotaDays(LocalDate.of(2023, 4, 1), 2026))
                .isEqualTo(14);
    }

    @Test
    void annualLeaveIncreasesAfterTenYearsAndCapsAtThirtyDays() {
        assertThat(leaveBalanceService.calculateAnnualLeaveQuotaDays(LocalDate.of(2014, 4, 1), 2026))
                .isEqualTo(17);
        assertThat(leaveBalanceService.calculateAnnualLeaveQuotaDays(LocalDate.of(1990, 4, 1), 2026))
                .isEqualTo(30);
    }

    @Test
    void annualLeaveHandlesLeapDayHireDate() {
        assertThat(leaveBalanceService.calculateAnnualLeaveQuotaDays(LocalDate.of(2024, 2, 29), 2024))
                .isEqualTo(3);
        assertThat(leaveBalanceService.calculateAnnualLeaveQuotaDays(LocalDate.of(2024, 2, 29), 2025))
                .isEqualTo(3);
    }
}
