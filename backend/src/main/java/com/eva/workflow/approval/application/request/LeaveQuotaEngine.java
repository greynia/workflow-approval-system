package com.eva.workflow.approval.application.request;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.application.exception.BadRequestApplicationException;
import com.eva.workflow.approval.application.exception.ResourceNotFoundApplicationException;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.CompanyWorkScheduleEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeScheduleEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.CompanyWorkScheduleRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.EmployeeScheduleRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.HolidayRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeaveQuotaEngine {

    private static final int MINIMUM_UNIT_MINUTES = 30;

    private final CompanyWorkScheduleRepository companyWorkScheduleRepository;
    private final HolidayRepository holidayRepository;
    private final EmployeeScheduleRepository employeeScheduleRepository;

    @Transactional(readOnly = true)
    public int calculateDurationMinutes(Long employeeId, LocalDateTime startTime, LocalDateTime endTime) {
        validateRequestWindow(startTime, endTime);

        CompanyWorkScheduleEntity companySchedule = companyWorkScheduleRepository
                .findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(startTime.toLocalDate())
                .orElseThrow(() -> new ResourceNotFoundApplicationException("Company work schedule not found"));

        EmployeeScheduleEntity employeeSchedule = employeeScheduleRepository
                .findActiveSchedules(employeeId, startTime.toLocalDate())
                .stream()
                .findFirst()
                .orElse(null);
        if (employeeSchedule != null && !"STANDARD".equals(employeeSchedule.getScheduleType())) {
            throw new BadRequestApplicationException("UNSUPPORTED_EMPLOYEE_SCHEDULE");
        }

        Set<LocalDate> holidays = holidayRepository.findByDateBetween(startTime.toLocalDate(), endTime.toLocalDate()).stream()
                .map(holiday -> holiday.getDate())
                .collect(Collectors.toSet());

        int totalMinutes = 0;
        for (LocalDate currentDate = startTime.toLocalDate();
             !currentDate.isAfter(endTime.toLocalDate());
             currentDate = currentDate.plusDays(1)) {
            if (holidays.contains(currentDate) || isNonWorkingDay(currentDate.getDayOfWeek(), companySchedule.getWorkDays())) {
                continue;
            }

            LocalDateTime workStart = currentDate.atTime(companySchedule.getWorkStart());
            LocalDateTime workEnd = currentDate.atTime(companySchedule.getWorkEnd());
            LocalDateTime effectiveStart = max(startTime, workStart);
            LocalDateTime effectiveEnd = min(endTime, workEnd);

            if (!effectiveEnd.isAfter(effectiveStart)) {
                continue;
            }

            totalMinutes += (int) ChronoUnit.MINUTES.between(effectiveStart, effectiveEnd);
            totalMinutes -= overlapMinutes(
                    effectiveStart,
                    effectiveEnd,
                    currentDate.atTime(companySchedule.getLunchStart()),
                    currentDate.atTime(companySchedule.getLunchEnd())
            );
        }

        if (totalMinutes < MINIMUM_UNIT_MINUTES || totalMinutes % MINIMUM_UNIT_MINUTES != 0) {
            throw new BadRequestApplicationException("INVALID_DURATION_UNIT");
        }

        return totalMinutes;
    }

    private void validateRequestWindow(LocalDateTime startTime, LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestApplicationException("endTime must be after startTime");
        }
        if (!isHalfHourAligned(startTime.toLocalTime()) || !isHalfHourAligned(endTime.toLocalTime())) {
            throw new BadRequestApplicationException("INVALID_DURATION_UNIT");
        }
    }

    private boolean isHalfHourAligned(LocalTime time) {
        return time.getSecond() == 0 && time.getNano() == 0 && (time.getMinute() == 0 || time.getMinute() == 30);
    }

    private boolean isNonWorkingDay(DayOfWeek dayOfWeek, String workDaysJson) {
        int isoDay = dayOfWeek.getValue();
        return !workDaysJson.contains(String.valueOf(isoDay));
    }

    private int overlapMinutes(
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            LocalDateTime lunchStart,
            LocalDateTime lunchEnd
    ) {
        LocalDateTime start = max(rangeStart, lunchStart);
        LocalDateTime end = min(rangeEnd, lunchEnd);
        if (!end.isAfter(start)) {
            return 0;
        }
        return (int) ChronoUnit.MINUTES.between(start, end);
    }

    private LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }

    private LocalDateTime min(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }
}
