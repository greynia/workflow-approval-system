package com.eva.workflow.approval.application.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eva.workflow.approval.api.dto.admin.CreateHolidayRequest;
import com.eva.workflow.approval.api.dto.admin.HolidayImportResultResponse;
import com.eva.workflow.approval.api.dto.admin.HolidayResponse;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.application.exception.BadRequestApplicationException;
import com.eva.workflow.approval.application.exception.ForbiddenApplicationException;
import com.eva.workflow.approval.application.exception.ResourceNotFoundApplicationException;
import com.eva.workflow.approval.common.enums.UserRole;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.HolidayEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.HolidayRepository;

@ExtendWith(MockitoExtension.class)
class AdminHolidayServiceTest {

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private TaiwanCalendarApiClient calendarApiClient;

    private AdminHolidayService adminHolidayService;

    private final AuthenticatedEmployee admin = new AuthenticatedEmployee(1L, "admin@example.com", "Admin", UserRole.ADMIN, List.of());
    private final AuthenticatedEmployee employee = new AuthenticatedEmployee(7L, "user@example.com", "User", UserRole.EMPLOYEE, List.of());

    @BeforeEach
    void setUp() {
        adminHolidayService = new AdminHolidayService(holidayRepository, calendarApiClient);
    }

    // ── getHolidaysByYear ──────────────────────────────────────────────────

    @Test
    void getHolidaysByYear_rejectsNonAdmin() {
        assertThatThrownBy(() -> adminHolidayService.getHolidaysByYear(employee, 2026))
                .isInstanceOf(ForbiddenApplicationException.class);
    }

    @Test
    void getHolidaysByYear_returnsResponsesSortedByDate() {
        HolidayEntity h1 = holidayEntity(1L, LocalDate.of(2026, 1, 1), "開國紀念日", 2026);
        HolidayEntity h2 = holidayEntity(2L, LocalDate.of(2026, 2, 28), "和平紀念日", 2026);
        when(holidayRepository.findByYearOrderByDateAsc(2026)).thenReturn(List.of(h1, h2));

        List<HolidayResponse> result = adminHolidayService.getHolidaysByYear(admin, 2026);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).date()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(result.get(0).name()).isEqualTo("開國紀念日");
        assertThat(result.get(1).date()).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    // ── addHoliday ────────────────────────────────────────────────────────

    @Test
    void addHoliday_rejectsNonAdmin() {
        assertThatThrownBy(() -> adminHolidayService.addHoliday(employee, new CreateHolidayRequest(LocalDate.of(2026, 1, 1), "Test")))
                .isInstanceOf(ForbiddenApplicationException.class);
        verify(holidayRepository, never()).insertIfDateAbsent(any(), any(), anyInt());
    }

    @Test
    void addHoliday_throwsConflictForDuplicateDate() {
        LocalDate date = LocalDate.of(2026, 1, 1);
        when(holidayRepository.insertIfDateAbsent(date, "開國紀念日", 2026)).thenReturn(0);

        assertThatThrownBy(() -> adminHolidayService.addHoliday(admin, new CreateHolidayRequest(date, "開國紀念日")))
                .isInstanceOf(BadRequestApplicationException.class)
                .hasMessageContaining("HOLIDAY_DATE_CONFLICT");
        verify(holidayRepository, never()).findByDate(any());
    }

    @Test
    void addHoliday_savesAndReturnsResponse() {
        LocalDate date = LocalDate.of(2026, 1, 1);
        HolidayEntity saved = holidayEntity(10L, date, "開國紀念日", 2026);
        when(holidayRepository.insertIfDateAbsent(date, "開國紀念日", 2026)).thenReturn(1);
        when(holidayRepository.findByDate(date)).thenReturn(Optional.of(saved));

        HolidayResponse result = adminHolidayService.addHoliday(admin, new CreateHolidayRequest(date, "開國紀念日"));

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.date()).isEqualTo(date);
        assertThat(result.name()).isEqualTo("開國紀念日");
        assertThat(result.year()).isEqualTo(2026);
        verify(holidayRepository, never()).save(any(HolidayEntity.class));
    }

    // ── importFromCalendar ────────────────────────────────────────────────

    @Test
    void importFromCalendar_rejectsNonAdmin() {
        assertThatThrownBy(() -> adminHolidayService.importFromCalendar(employee, 2026))
                .isInstanceOf(ForbiddenApplicationException.class);
        verify(calendarApiClient, never()).fetchHolidaysForYear(any(int.class));
    }

    @Test
    void importFromCalendar_insertsNewHolidaysAndSkipsExisting() {
        LocalDate existing = LocalDate.of(2026, 1, 1);
        LocalDate newDate = LocalDate.of(2026, 2, 28);
        List<TaiwanCalendarApiClient.TaiwanCalendarDay> days = List.of(
                new TaiwanCalendarApiClient.TaiwanCalendarDay("20260101", "四", true, "開國紀念日"),
                new TaiwanCalendarApiClient.TaiwanCalendarDay("20260228", "六", true, "和平紀念日")
        );
        when(calendarApiClient.fetchHolidaysForYear(2026)).thenReturn(days);
        when(holidayRepository.insertIfDateAbsent(eq(existing), eq("開國紀念日"), eq(2026))).thenReturn(0);
        when(holidayRepository.insertIfDateAbsent(eq(newDate), eq("和平紀念日"), eq(2026))).thenReturn(1);

        HolidayImportResultResponse result = adminHolidayService.importFromCalendar(admin, 2026);

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        verify(holidayRepository, times(2)).insertIfDateAbsent(any(), any(), anyInt());
    }

    @Test
    void importFromCalendar_isIdempotentWhenAllDatesExist() {
        List<TaiwanCalendarApiClient.TaiwanCalendarDay> days = List.of(
                new TaiwanCalendarApiClient.TaiwanCalendarDay("20260101", "四", true, "開國紀念日"),
                new TaiwanCalendarApiClient.TaiwanCalendarDay("20260228", "六", true, "和平紀念日")
        );
        when(calendarApiClient.fetchHolidaysForYear(2026)).thenReturn(days);
        when(holidayRepository.insertIfDateAbsent(any(), any(), anyInt())).thenReturn(0);

        HolidayImportResultResponse result = adminHolidayService.importFromCalendar(admin, 2026);

        assertThat(result.imported()).isEqualTo(0);
        assertThat(result.skipped()).isEqualTo(2);
        verify(holidayRepository, never()).save(any());
    }

    // ── deleteHoliday ─────────────────────────────────────────────────────

    @Test
    void deleteHoliday_rejectsNonAdmin() {
        assertThatThrownBy(() -> adminHolidayService.deleteHoliday(employee, 1L))
                .isInstanceOf(ForbiddenApplicationException.class);
        verify(holidayRepository, never()).delete(any());
    }

    @Test
    void deleteHoliday_throwsNotFoundForMissingId() {
        when(holidayRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminHolidayService.deleteHoliday(admin, 9999L))
                .isInstanceOf(ResourceNotFoundApplicationException.class);
        verify(holidayRepository, never()).delete(any());
    }

    @Test
    void deleteHoliday_deletesEntity() {
        HolidayEntity entity = holidayEntity(5L, LocalDate.of(2026, 1, 1), "開國紀念日", 2026);
        when(holidayRepository.findById(5L)).thenReturn(Optional.of(entity));

        adminHolidayService.deleteHoliday(admin, 5L);

        verify(holidayRepository).delete(entity);
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private HolidayEntity holidayEntity(Long id, LocalDate date, String name, int year) {
        HolidayEntity entity = HolidayEntity.create(date, name, year);
        setField(entity, "id", id);
        return entity;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
