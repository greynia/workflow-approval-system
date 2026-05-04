package com.eva.workflow.approval.domain.aireview.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.common.enums.RiskLevel;
import com.eva.workflow.approval.common.enums.UserRole;
import com.eva.workflow.approval.domain.aireview.model.HardRuleFlag;
import com.eva.workflow.approval.domain.aireview.model.ReviewSnapshot;

class AiHardRuleEngineTest {

    private AiHardRuleEngine hardRuleEngine;

    @BeforeEach
    void setUp() {
        hardRuleEngine = new AiHardRuleEngine();
    }

    @Test
    void evaluateMarksNewHireShortLeaveAsMedium() {
        List<HardRuleFlag> flags = hardRuleEngine.evaluate(snapshot(LeaveType.ANNUAL, 480, 30, 1, 1));

        assertThat(flags).contains(new HardRuleFlag(
                "NEW_HIRE_SHORT_LEAVE",
                RiskLevel.MEDIUM,
                "New hire requests leave within first 90 days"));
        assertThat(hardRuleEngine.highestRiskLevel(flags)).isEqualTo(RiskLevel.MEDIUM);
    }

    @Test
    void evaluateMarksNewHireLongLeaveAsHigh() {
        List<HardRuleFlag> flags = hardRuleEngine.evaluate(snapshot(LeaveType.ANNUAL, 960, 30, 1, 2));

        assertThat(flags).contains(new HardRuleFlag(
                "NEW_HIRE_LONG_LEAVE",
                RiskLevel.HIGH,
                "New hire requests more than 1 workday of leave"));
        assertThat(hardRuleEngine.highestRiskLevel(flags)).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void evaluateMarksLongSickLeaveAsMediumForNonNewHire() {
        List<HardRuleFlag> flags = hardRuleEngine.evaluate(snapshot(LeaveType.SICK, 2400, 800, 1, 1));

        assertThat(flags).contains(new HardRuleFlag(
                "LONG_DURATION",
                RiskLevel.MEDIUM,
                "Sick or other leave exceeds 3 days"));
        assertThat(hardRuleEngine.highestRiskLevel(flags)).isEqualTo(RiskLevel.MEDIUM);
    }

    @Test
    void evaluateMarksFrequentLeaveAsMedium() {
        List<HardRuleFlag> flags = hardRuleEngine.evaluate(snapshot(LeaveType.ANNUAL, 480, 800, 5, 1));

        assertThat(flags).contains(new HardRuleFlag(
                "FREQUENT_LEAVE_30D",
                RiskLevel.MEDIUM,
                "Applied 5+ times in the last 30 days"));
        assertThat(hardRuleEngine.highestRiskLevel(flags)).isEqualTo(RiskLevel.MEDIUM);
    }

    @Test
    void evaluateDoesNotMarkLongAnnualOrPersonalLeaveAsLongDuration() {
        List<HardRuleFlag> annualFlags = hardRuleEngine.evaluate(snapshot(LeaveType.ANNUAL, 2400, 800, 1, 1));
        List<HardRuleFlag> personalFlags = hardRuleEngine.evaluate(snapshot(LeaveType.PERSONAL, 2400, 800, 1, 1));

        assertThat(annualFlags)
                .extracting(HardRuleFlag::code)
                .doesNotContain("LONG_DURATION");
        assertThat(personalFlags)
                .extracting(HardRuleFlag::code)
                .doesNotContain("LONG_DURATION");
    }

    @Test
    void evaluateKeepsNewHireLongSickLeaveAtHigh() {
        List<HardRuleFlag> flags = hardRuleEngine.evaluate(snapshot(LeaveType.SICK, 2400, 30, 1, 2));

        assertThat(flags).contains(
                new HardRuleFlag(
                        "NEW_HIRE_LONG_LEAVE",
                        RiskLevel.HIGH,
                        "New hire requests more than 1 workday of leave"),
                new HardRuleFlag(
                        "LONG_DURATION",
                        RiskLevel.MEDIUM,
                        "Sick or other leave exceeds 3 days")
        );
        assertThat(hardRuleEngine.highestRiskLevel(flags)).isEqualTo(RiskLevel.HIGH);
    }

    private ReviewSnapshot snapshot(
            LeaveType leaveType,
            int durationMinutes,
            long applicantTenureDays,
            int recentLeaveCountLast30Days,
            int approvalStepCount
    ) {
        return new ReviewSnapshot(
                10L,
                leaveType,
                LocalDateTime.of(2026, 5, 7, 9, 0),
                LocalDateTime.of(2026, 5, 7, 18, 0),
                durationMinutes,
                "Hard rule test",
                Instant.parse("2026-04-29T10:00:00Z"),
                7L,
                applicantTenureDays,
                UserRole.EMPLOYEE,
                "後端組",
                recentLeaveCountLast30Days,
                approvalStepCount
        );
    }
}
