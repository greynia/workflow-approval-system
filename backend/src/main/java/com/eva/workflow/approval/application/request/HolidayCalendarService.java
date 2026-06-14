package com.eva.workflow.approval.application.request;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.infrastructure.cache.CacheNames;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.HolidayRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HolidayCalendarService {

    private final HolidayRepository holidayRepository;

    @Cacheable(value = CacheNames.HOLIDAY_CALENDAR, key = "#startDate + ':' + #endDate")
    @Transactional(readOnly = true)
    public Set<LocalDate> getHolidayDatesBetween(LocalDate startDate, LocalDate endDate) {
        return holidayRepository.findByDateBetween(startDate, endDate).stream()
                .map(holiday -> holiday.getDate())
                .collect(Collectors.toSet());
    }
}
