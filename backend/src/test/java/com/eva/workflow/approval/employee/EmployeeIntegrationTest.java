package com.eva.workflow.approval.employee;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.eva.workflow.approval.TestcontainersConfiguration;

import jakarta.servlet.http.Cookie;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class EmployeeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void employeesListReturnsActiveEmployeesExcludingCurrentUser() throws Exception {
        Cookie tokenCookie = loginAndGetCookie("huang.yating@example.com", "password123");

        mockMvc.perform(get("/api/employees")
                        .cookie(tokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(7))))
                .andExpect(jsonPath("$[*].name", hasItem("李建國")))
                .andExpect(jsonPath("$[*].name", hasItem("張美玲")));
    }

    @Test
    void availableDeputiesExcludesEmployeesWithOverlappingLeave() throws Exception {
        Cookie applicantCookie = loginAndGetCookie("huang.yating@example.com", "password123");
        Cookie deputyCookie = loginAndGetCookie("li.jianguo@example.com", "FrontendDev123!");

        mockMvc.perform(post("/api/requests")
                        .cookie(deputyCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(
                                "ANNUAL",
                                "2026-08-03T09:00:00",
                                "2026-08-05T18:00:00",
                                "Deputy is away",
                                7
                        )))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/employees/available-deputies")
                        .cookie(applicantCookie)
                        .param("startTime", "2026-08-04T09:00:00")
                        .param("endTime", "2026-08-05T18:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(6))))
                .andExpect(jsonPath("$[*].name", hasItem("張美玲")));
    }

    @Test
    void availableDeputiesRejectsInvalidDateRange() throws Exception {
        Cookie tokenCookie = loginAndGetCookie("huang.yating@example.com", "password123");

        mockMvc.perform(get("/api/employees/available-deputies")
                        .cookie(tokenCookie)
                        .param("startTime", "2026-08-05T18:00:00")
                        .param("endTime", "2026-08-05T09:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("endTime must be after startTime"));
    }

    private String requestBody(String type, String startTime, String endTime, String reason, int deputyId) {
        return """
                {
                  "type": "%s",
                  "startTime": "%s",
                  "endTime": "%s",
                  "reason": "%s",
                  "deputyId": %d
                }
                """.formatted(type, startTime, endTime, reason, deputyId);
    }

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
