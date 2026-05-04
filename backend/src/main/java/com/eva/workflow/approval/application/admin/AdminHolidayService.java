package com.eva.workflow.approval.application.admin;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.api.dto.admin.CreateHolidayRequest;
import com.eva.workflow.approval.api.dto.admin.HolidayImportResultResponse;
import com.eva.workflow.approval.api.dto.admin.HolidayResponse;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.application.exception.BadRequestApplicationException;
import com.eva.workflow.approval.application.exception.ForbiddenApplicationException;
import com.eva.workflow.approval.application.exception.ResourceNotFoundApplicationException;
import com.eva.workflow.approval.common.enums.UserRole;
import com.eva.workflow.approval.infrastructure.cache.CacheNames;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.HolidayEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.HolidayRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminHolidayService {

    private final HolidayRepository holidayRepository;
    private final TaiwanCalendarApiClient calendarApiClient;

    @Transactional(readOnly = true)
    public List<HolidayResponse> getHolidaysByYear(AuthenticatedEmployee actor, int year) {
        requireAdmin(actor);
        return holidayRepository.findByYearOrderByDateAsc(year).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(value = CacheNames.HOLIDAY_CALENDAR, allEntries = true)
    public HolidayResponse addHoliday(AuthenticatedEmployee actor, CreateHolidayRequest request) {
        requireAdmin(actor);
        int inserted = holidayRepository.insertIfDateAbsent(request.date(), request.name(), request.date().getYear());
        if (inserted == 0) {
            throw new BadRequestApplicationException("HOLIDAY_DATE_CONFLICT");
        }
        HolidayEntity entity = holidayRepository.findByDate(request.date())
                .orElseThrow(() -> new ResourceNotFoundApplicationException("Holiday not found after insert"));
        return toResponse(entity);
    }

    @Transactional
    @CacheEvict(value = CacheNames.HOLIDAY_CALENDAR, allEntries = true)
    public HolidayImportResultResponse importFromCalendar(AuthenticatedEmployee actor, int year) {
        requireAdmin(actor);
        List<TaiwanCalendarApiClient.TaiwanCalendarDay> holidays = calendarApiClient.fetchHolidaysForYear(year);

        int imported = 0;
        int skipped = 0;
        for (TaiwanCalendarApiClient.TaiwanCalendarDay day : holidays) {
            int inserted = holidayRepository.insertIfDateAbsent(day.toLocalDate(), day.description(), year);
            if (inserted == 0) {
                skipped++;
            } else {
                imported++;
            }
        }
        return new HolidayImportResultResponse(imported, skipped);
    }

    @Transactional
    @CacheEvict(value = CacheNames.HOLIDAY_CALENDAR, allEntries = true)
    public void deleteHoliday(AuthenticatedEmployee actor, Long id) {
        requireAdmin(actor);
        HolidayEntity entity = holidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundApplicationException("Holiday not found"));
        holidayRepository.delete(entity);
    }

    private HolidayResponse toResponse(HolidayEntity entity) {
        return new HolidayResponse(entity.getId(), entity.getDate(), entity.getName(), entity.getYear());
    }

    private void requireAdmin(AuthenticatedEmployee actor) {
        if (actor.role() != UserRole.ADMIN) {
            throw new ForbiddenApplicationException("Only admins can manage holidays");
        }
    }
}
