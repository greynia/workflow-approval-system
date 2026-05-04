package com.eva.workflow.approval.application.admin;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.eva.workflow.approval.application.exception.ApplicationConfigurationException;

@Component
public class TaiwanCalendarApiClient {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RestClient restClient;

    public TaiwanCalendarApiClient(@Value("${app.taiwan-calendar.base-url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public List<TaiwanCalendarDay> fetchHolidaysForYear(int year) {
        try {
            List<TaiwanCalendarDay> days = restClient.get()
                    .uri("/{year}.json", year)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (days == null) {
                throw new ApplicationConfigurationException("CALENDAR_API_UNAVAILABLE");
            }
            return days.stream()
                    .filter(TaiwanCalendarDay::isHoliday)
                    .filter(d -> d.description() != null && !d.description().isBlank())
                    .peek(TaiwanCalendarDay::toLocalDate)
                    .toList();
        } catch (RestClientException | DateTimeParseException | IllegalArgumentException e) {
            throw new ApplicationConfigurationException("CALENDAR_API_UNAVAILABLE");
        }
    }

    public record TaiwanCalendarDay(String date, String week, boolean isHoliday, String description) {

        public LocalDate toLocalDate() {
            if (date == null) {
                throw new IllegalArgumentException("Calendar date is missing");
            }
            return LocalDate.parse(date, DATE_FORMAT);
        }
    }
}
