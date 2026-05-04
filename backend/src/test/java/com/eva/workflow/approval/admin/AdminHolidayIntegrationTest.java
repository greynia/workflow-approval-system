package com.eva.workflow.approval.admin;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.eva.workflow.approval.TestcontainersConfiguration;
import com.eva.workflow.approval.application.admin.TaiwanCalendarApiClient;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

@Import({TestcontainersConfiguration.class, AdminHolidayIntegrationTest.FixedClockConfig.class})
@SpringBootTest
@AutoConfigureMockMvc
class AdminHolidayIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaiwanCalendarApiClient calendarApiClient;

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-12-31T16:30:00Z"), ZoneId.of("UTC"));
        }
    }

    // ── GET /api/admin/holidays ───────────────────────────────────────────

    @Test
    void getHolidays_rejectsNonAdmin() throws Exception {
        Cookie employeeCookie = loginAndGetCookie("huang.yating@example.com", "password123");

        mockMvc.perform(get("/api/admin/holidays").cookie(employeeCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getHolidays_returnsEmptyListForUnusedYear() throws Exception {
        Cookie adminCookie = loginAndGetCookie("admin@example.com", "AdminPass123!");

        mockMvc.perform(get("/api/admin/holidays")
                        .cookie(adminCookie)
                        .param("year", "2099"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getHolidays_defaultsToCurrentTaipeiYear() throws Exception {
        Cookie adminCookie = loginAndGetCookie("admin@example.com", "AdminPass123!");

        mockMvc.perform(post("/api/admin/holidays")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date": "2027-12-31", "name": "台北預設年份測試假日"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/holidays")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("台北預設年份測試假日")));
    }

    // ── POST /api/admin/holidays ──────────────────────────────────────────

    @Test
    void addHoliday_rejectsNonAdmin() throws Exception {
        Cookie employeeCookie = loginAndGetCookie("huang.yating@example.com", "password123");

        mockMvc.perform(post("/api/admin/holidays")
                        .cookie(employeeCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date": "2098-06-01", "name": "Test Holiday"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void addHoliday_createsHolidayAndReturns201() throws Exception {
        Cookie adminCookie = loginAndGetCookie("admin@example.com", "AdminPass123!");

        mockMvc.perform(post("/api/admin/holidays")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date": "2097-06-01", "name": "測試假日"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.date").value("2097-06-01"))
                .andExpect(jsonPath("$.name").value("測試假日"))
                .andExpect(jsonPath("$.year").value(2097))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void addHoliday_rejectsDuplicateDate() throws Exception {
        Cookie adminCookie = loginAndGetCookie("admin@example.com", "AdminPass123!");
        String body = """
                {"date": "2096-07-04", "name": "自訂假日"}
                """;

        mockMvc.perform(post("/api/admin/holidays")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/holidays")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    // ── DELETE /api/admin/holidays/{id} ──────────────────────────────────

    @Test
    void deleteHoliday_returns204AndHolidayIsGone() throws Exception {
        Cookie adminCookie = loginAndGetCookie("admin@example.com", "AdminPass123!");

        MvcResult created = mockMvc.perform(post("/api/admin/holidays")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date": "2095-08-15", "name": "測試刪除假日"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        Number id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(delete("/api/admin/holidays/{id}", id.longValue())
                        .cookie(adminCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/holidays")
                        .cookie(adminCookie)
                        .param("year", "2095"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.not(hasItem(id.intValue()))));
    }

    @Test
    void deleteHoliday_returns404ForUnknownId() throws Exception {
        Cookie adminCookie = loginAndGetCookie("admin@example.com", "AdminPass123!");

        mockMvc.perform(delete("/api/admin/holidays/9999999")
                        .cookie(adminCookie))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/admin/holidays/import ──────────────────────────────────

    @Test
    void importHolidays_insertsNewHolidaysAndSkipsExisting() throws Exception {
        Cookie adminCookie = loginAndGetCookie("admin@example.com", "AdminPass123!");

        mockMvc.perform(post("/api/admin/holidays")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date": "2094-01-01", "name": "元旦"}
                                """))
                .andExpect(status().isCreated());

        Mockito.when(calendarApiClient.fetchHolidaysForYear(2094)).thenReturn(List.of(
                new TaiwanCalendarApiClient.TaiwanCalendarDay("20940101", "四", true, "元旦"),
                new TaiwanCalendarApiClient.TaiwanCalendarDay("20940228", "六", true, "和平紀念日")
        ));

        mockMvc.perform(post("/api/admin/holidays/import")
                        .cookie(adminCookie)
                        .param("year", "2094"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skipped").value(1));

        mockMvc.perform(get("/api/admin/holidays")
                        .cookie(adminCookie)
                        .param("year", "2094"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].name", hasItem("和平紀念日")));
    }

    @Test
    void importHolidays_isIdempotentOnRepeatCall() throws Exception {
        Cookie adminCookie = loginAndGetCookie("admin@example.com", "AdminPass123!");

        Mockito.when(calendarApiClient.fetchHolidaysForYear(2093)).thenReturn(List.of(
                new TaiwanCalendarApiClient.TaiwanCalendarDay("20930101", "三", true, "元旦")
        ));

        mockMvc.perform(post("/api/admin/holidays/import")
                        .cookie(adminCookie)
                        .param("year", "2093"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skipped").value(0));

        mockMvc.perform(post("/api/admin/holidays/import")
                        .cookie(adminCookie)
                        .param("year", "2093"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(0))
                .andExpect(jsonPath("$.skipped").value(1));
    }

    @Test
    void importHolidays_defaultsToCurrentTaipeiYear() throws Exception {
        Cookie adminCookie = loginAndGetCookie("admin@example.com", "AdminPass123!");

        Mockito.when(calendarApiClient.fetchHolidaysForYear(2027)).thenReturn(List.of(
                new TaiwanCalendarApiClient.TaiwanCalendarDay("20270101", "五", true, "元旦")
        ));

        mockMvc.perform(post("/api/admin/holidays/import")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skipped").value(0));

        Mockito.verify(calendarApiClient).fetchHolidaysForYear(2027);
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private Cookie loginAndGetCookie(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        return result.getResponse().getCookie("workflow-token");
    }
}
