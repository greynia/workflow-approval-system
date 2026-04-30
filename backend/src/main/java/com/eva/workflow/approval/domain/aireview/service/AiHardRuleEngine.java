package com.eva.workflow.approval.domain.aireview.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.common.enums.RiskLevel;
import com.eva.workflow.approval.domain.aireview.model.HardRuleFlag;
import com.eva.workflow.approval.domain.aireview.model.ReviewSnapshot;

@Component
public class AiHardRuleEngine {

    private static final int FREQUENT_LEAVE_THRESHOLD = 5;
    private static final int NEW_HIRE_TENURE_DAYS = 90;
    private static final int NEW_HIRE_SHORT_LEAVE_MAX_MINUTES = 480;
    private static final int LONG_DURATION_MINUTES = 1440;
    private static final Set<LeaveType> LONG_DURATION_LEAVE_TYPES = EnumSet.of(LeaveType.SICK, LeaveType.OTHER);

    public List<HardRuleFlag> evaluate(ReviewSnapshot snapshot) {
        return evaluate(snapshot, "en");
    }

    public List<HardRuleFlag> evaluate(ReviewSnapshot snapshot, String locale) {
        List<HardRuleFlag> flags = new ArrayList<>();

        if (snapshot.recentLeaveCountLast30Days() >= FREQUENT_LEAVE_THRESHOLD) {
            flags.add(new HardRuleFlag(
                    "FREQUENT_LEAVE_30D",
                    RiskLevel.MEDIUM,
                    message("FREQUENT_LEAVE_30D", locale)));
        }

        if (snapshot.applicantTenureDays() < NEW_HIRE_TENURE_DAYS) {
            if (snapshot.durationMinutes() > NEW_HIRE_SHORT_LEAVE_MAX_MINUTES) {
                flags.add(new HardRuleFlag(
                        "NEW_HIRE_LONG_LEAVE",
                        RiskLevel.HIGH,
                        message("NEW_HIRE_LONG_LEAVE", locale)));
            } else {
                flags.add(new HardRuleFlag(
                        "NEW_HIRE_SHORT_LEAVE",
                        RiskLevel.MEDIUM,
                        message("NEW_HIRE_SHORT_LEAVE", locale)));
            }
        }

        if (LONG_DURATION_LEAVE_TYPES.contains(snapshot.leaveType())
                && snapshot.durationMinutes() > LONG_DURATION_MINUTES) {
            flags.add(new HardRuleFlag(
                    "LONG_DURATION",
                    RiskLevel.MEDIUM,
                    message("LONG_DURATION", locale)));
        }

        return Collections.unmodifiableList(flags);
    }

    public RiskLevel highestRiskLevel(List<HardRuleFlag> flags) {
        return flags.stream()
                .map(HardRuleFlag::level)
                .max(Enum::compareTo)
                .orElse(RiskLevel.LOW);
    }

    private String message(String code, String locale) {
        boolean zh = locale != null && locale.toLowerCase().startsWith("zh");
        if (zh) {
            return switch (code) {
                case "FREQUENT_LEAVE_30D" -> "過去 30 天內已申請 5 次以上請假";
                case "NEW_HIRE_LONG_LEAVE" -> "新進員工申請超過 1 個工作天的請假";
                case "NEW_HIRE_SHORT_LEAVE" -> "新進員工在到職 90 天內申請請假";
                case "LONG_DURATION" -> "病假或其他假超過 3 天";
                default -> code;
            };
        }

        return switch (code) {
            case "FREQUENT_LEAVE_30D" -> "Applied 5+ times in the last 30 days";
            case "NEW_HIRE_LONG_LEAVE" -> "New hire requests more than 1 workday of leave";
            case "NEW_HIRE_SHORT_LEAVE" -> "New hire requests leave within first 90 days";
            case "LONG_DURATION" -> "Sick or other leave exceeds 3 days";
            default -> code;
        };
    }
}
