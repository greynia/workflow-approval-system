package com.eva.workflow.approval.request;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class LeaveRequestIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createRequestPersistsPendingLeaveRequestWithDeputyFirstFlow() throws Exception {
        Cookie tokenCookie = loginAndGetCookie("huang.yating@example.com", "password123");

        mockMvc.perform(post("/api/requests")
                        .cookie(tokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(
                                "SICK",
                                "2026-05-20T09:00:00",
                                "2026-05-22T18:00:00",
                                "Family trip",
                                6
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicantId").value(7))
                .andExpect(jsonPath("$.deputyId").value(6))
                .andExpect(jsonPath("$.durationMinutes").value(1440))
                .andExpect(jsonPath("$.currentStage").value("WAITING_DEPUTY"))
                .andExpect(jsonPath("$.approvalSteps.length()").value(2))
                .andExpect(jsonPath("$.approvalSteps[0].stepType").value("DEPUTY"))
                .andExpect(jsonPath("$.approvalSteps[0].approverId").value(6))
                .andExpect(jsonPath("$.approvalSteps[1].stepType").value("MANAGER"))
                .andExpect(jsonPath("$.approvalSteps[1].approverId").value(5));
    }

    @Test
    void createRequestGeneratesThreeStepsWhenDurationExceedsOneDayThreshold() throws Exception {
        Cookie tokenCookie = loginAndGetCookie("huang.yating@example.com", "password123");

        mockMvc.perform(post("/api/requests")
                        .cookie(tokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(
                                "SICK",
                                "2026-06-01T09:00:00",
                                "2026-06-05T18:00:00",
                                "Long trip",
                                6
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.durationMinutes").value(2400))
                .andExpect(jsonPath("$.approvalSteps.length()").value(3))
                .andExpect(jsonPath("$.approvalSteps[0].stepType").value("DEPUTY"))
                .andExpect(jsonPath("$.approvalSteps[1].stepType").value("MANAGER"))
                .andExpect(jsonPath("$.approvalSteps[2].stepType").value("MANAGER"))
                .andExpect(jsonPath("$.approvalSteps[2].approverId").value(2));
    }

    @Test
    void calculateReturnsDurationExcludingLunchAndWeekend() throws Exception {
        Cookie tokenCookie = loginAndGetCookie("huang.yating@example.com", "password123");

        mockMvc.perform(post("/api/requests/calculate")
                        .cookie(tokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startTime": "2026-04-23T09:00:00",
                                  "endTime": "2026-04-27T18:00:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationMinutes").value(1440));
    }

    @Test
    void createRejectsDeputySameAsApplicant() throws Exception {
        Cookie tokenCookie = loginAndGetCookie("huang.yating@example.com", "password123");

        mockMvc.perform(post("/api/requests")
                        .cookie(tokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(
                                "SICK",
                                "2026-04-20T09:00:00",
                                "2026-04-20T18:00:00",
                                "One day off",
                                7
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void createRejectsDeputyWhenDeputyHasOverlappingLeave() throws Exception {
        Cookie deputyCookie = loginAndGetCookie("li.jianguo@example.com", "FrontendDev123!");
        Cookie applicantCookie = loginAndGetCookie("huang.yating@example.com", "password123");

        createRequest(deputyCookie, requestBody(
                "SICK",
                "2026-06-10T09:00:00",
                "2026-06-12T18:00:00",
                "Deputy is away",
                7
        ));

        mockMvc.perform(post("/api/requests")
                        .cookie(applicantCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(
                                "SICK",
                                "2026-06-11T09:00:00",
                                "2026-06-12T18:00:00",
                                "Applicant leave",
                                6
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("DEPUTY_ON_LEAVE"));
    }

    @Test
    void createRejectsApplicantWhenApplicantHasOverlappingLeave() throws Exception {
        Cookie applicantCookie = loginAndGetCookie("huang.yating@example.com", "password123");

        createRequest(applicantCookie, requestBody(
                "SICK",
                "2026-06-15T09:00:00",
                "2026-06-17T18:00:00",
                "First leave",
                6
        ));

        mockMvc.perform(post("/api/requests")
                        .cookie(applicantCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(
                                "SICK",
                                "2026-06-16T09:00:00",
                                "2026-06-17T18:00:00",
                                "Overlapping leave",
                                6
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("APPLICANT_ON_LEAVE"));
    }

    @Test
    void cancelPendingRequestMarksStatusCancelledAndSkipsPendingSteps() throws Exception {
        Cookie applicantCookie = loginAndGetCookie("huang.yating@example.com", "password123");
        assertAnnualBalance(applicantCookie, 2026, 4800, 0, 4800);

        Long requestId = createRequest(applicantCookie, requestBody(
                "ANNUAL",
                "2026-07-01T09:00:00",
                "2026-07-01T18:00:00",
                "Cancelable",
                6
        ));

        assertAnnualBalance(applicantCookie, 2026, 4800, 480, 4320);

        mockMvc.perform(patch("/api/requests/{id}/cancel", requestId)
                        .cookie(applicantCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/requests/{id}", requestId)
                        .cookie(applicantCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.approvalSteps[*].status", hasItem("SKIPPED")));

        assertAnnualBalance(applicantCookie, 2026, 4800, 0, 4800);
    }

    @Test
    void listRejectsInvalidPaginationArguments() throws Exception {
        Cookie tokenCookie = loginAndGetCookie("huang.yating@example.com", "password123");

        mockMvc.perform(get("/api/requests")
                        .cookie(tokenCookie)
                        .param("page", "-1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
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

    private Long createRequest(Cookie tokenCookie, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/requests")
                        .cookie(tokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        Number requestId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return requestId.longValue();
    }

    private void assertAnnualBalance(Cookie tokenCookie, int year, int quotaMinutes, int usedMinutes, int remainingMinutes)
            throws Exception {
        mockMvc.perform(get("/api/requests/balance")
                        .cookie(tokenCookie)
                        .param("year", String.valueOf(year)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.leaveType == 'ANNUAL')].quotaMinutes").value(hasItem(quotaMinutes)))
                .andExpect(jsonPath("$[?(@.leaveType == 'ANNUAL')].usedMinutes").value(hasItem(usedMinutes)))
                .andExpect(jsonPath("$[?(@.leaveType == 'ANNUAL')].remainingMinutes").value(hasItem(remainingMinutes)));
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
