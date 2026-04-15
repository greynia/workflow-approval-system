package com.eva.workflow.approval.approval;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

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
class ApprovalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pendingListReturnsDeputyStepBeforeManager() throws Exception {
        Cookie employeeCookie = loginAndGetCookie("huang.yating@example.com", "password123");
        Cookie deputyCookie = loginAndGetCookie("li.jianguo@example.com", "FrontendDev123!");
        Cookie managerCookie = loginAndGetCookie("zhang.meiling@example.com", "BackendLead123!");

        Long requestId = createRequest(employeeCookie, requestBody(
                "SICK",
                "2026-04-27T09:00:00",
                "2026-04-29T18:00:00",
                "Family trip",
                6
        ));

        mockMvc.perform(get("/api/approvals/pending")
                        .cookie(deputyCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].requestId", hasItem(requestId.intValue())))
                .andExpect(jsonPath("$[*].stepType", hasItem("DEPUTY")));

        mockMvc.perform(get("/api/approvals/pending")
                        .cookie(managerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].requestId", not(hasItem(requestId.intValue()))));
    }

    @Test
    void approveDeputyThenManagerMarksShortRequestApproved() throws Exception {
        Cookie employeeCookie = loginAndGetCookie("huang.yating@example.com", "password123");
        Cookie deputyCookie = loginAndGetCookie("li.jianguo@example.com", "FrontendDev123!");
        Cookie managerCookie = loginAndGetCookie("zhang.meiling@example.com", "BackendLead123!");

        Long requestId = createRequest(employeeCookie, requestBody(
                "SICK",
                "2026-04-20T09:00:00",
                "2026-04-22T18:00:00",
                "Family trip",
                6
        ));

        Long deputyStepId = getPendingStepId(deputyCookie, requestId);
        approve(deputyCookie, deputyStepId, "deputy approved");

        mockMvc.perform(get("/api/approvals/pending")
                        .cookie(managerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].requestId", hasItem(requestId.intValue())))
                .andExpect(jsonPath("$[*].stepType", hasItem("MANAGER")));

        Long managerStepId = getPendingStepId(managerCookie, requestId);
        approve(managerCookie, managerStepId, "manager approved");

        mockMvc.perform(get("/api/requests/{id}", requestId)
                        .cookie(employeeCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvalActions.length()").value(2));
    }

    @Test
    void approveDeputyThenTwoManagersMarksLongRequestApproved() throws Exception {
        Cookie employeeCookie = loginAndGetCookie("huang.yating@example.com", "password123");
        Cookie deputyCookie = loginAndGetCookie("li.jianguo@example.com", "FrontendDev123!");
        Cookie firstManagerCookie = loginAndGetCookie("zhang.meiling@example.com", "BackendLead123!");
        Cookie secondManagerCookie = loginAndGetCookie("chen.daming@example.com", "ManagerPass123!");

        Long requestId = createRequest(employeeCookie, requestBody(
                "SICK",
                "2026-05-04T09:00:00",
                "2026-05-07T18:00:00",
                "Long trip",
                6
        ));

        approve(deputyCookie, getPendingStepId(deputyCookie, requestId), "deputy ok");
        approve(firstManagerCookie, getPendingStepId(firstManagerCookie, requestId), "manager ok");

        mockMvc.perform(get("/api/approvals/pending")
                        .cookie(secondManagerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].requestId", hasItem(requestId.intValue())));

        approve(secondManagerCookie, getPendingStepId(secondManagerCookie, requestId), "director ok");

        mockMvc.perform(get("/api/requests/{id}", requestId)
                        .cookie(employeeCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvalSteps.length()").value(3));
    }

    @Test
    void secondManagerCannotSeeOrProcessStepBeforePreviousStepsComplete() throws Exception {
        Cookie employeeCookie = loginAndGetCookie("huang.yating@example.com", "password123");
        Cookie deputyCookie = loginAndGetCookie("li.jianguo@example.com", "FrontendDev123!");
        Cookie secondManagerCookie = loginAndGetCookie("chen.daming@example.com", "ManagerPass123!");

        Long requestId = createRequest(employeeCookie, requestBody(
                "SICK",
                "2026-05-11T09:00:00",
                "2026-05-14T18:00:00",
                "Long trip",
                6
        ));

        mockMvc.perform(get("/api/approvals/pending")
                        .cookie(secondManagerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].requestId", not(hasItem(requestId.intValue()))));

        Long secondStepId = getStepIdByIndex(employeeCookie, requestId, 2);
        mockMvc.perform(post("/api/approvals/{stepId}/reject", secondStepId)
                        .cookie(secondManagerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "comment": "trying to skip ahead"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Previous approval step has not been completed"));

        approve(deputyCookie, getPendingStepId(deputyCookie, requestId), "deputy ok");
    }

    @Test
    void deputyRejectRequestMarksRequestRejectedAndSkipsRemainingSteps() throws Exception {
        Cookie employeeCookie = loginAndGetCookie("huang.yating@example.com", "password123");
        Cookie deputyCookie = loginAndGetCookie("li.jianguo@example.com", "FrontendDev123!");
        Cookie managerCookie = loginAndGetCookie("zhang.meiling@example.com", "BackendLead123!");

        assertAnnualBalance(employeeCookie, 2026, 4800, 0, 4800);

        Long requestId = createRequest(employeeCookie, requestBody(
                "ANNUAL",
                "2026-07-06T09:00:00",
                "2026-07-08T18:00:00",
                "Need coverage",
                6
        ));

        assertAnnualBalance(employeeCookie, 2026, 4800, 1440, 3360);

        mockMvc.perform(post("/api/approvals/{stepId}/reject", getPendingStepId(deputyCookie, requestId))
                        .cookie(deputyCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "comment": "cannot cover"
                                }
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/requests/{id}", requestId)
                        .cookie(employeeCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.approvalSteps[0].status").value("REJECTED"))
                .andExpect(jsonPath("$.approvalSteps[1].status").value("SKIPPED"));

        mockMvc.perform(get("/api/approvals/pending")
                        .cookie(managerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].requestId", not(hasItem(requestId.intValue()))));

        assertAnnualBalance(employeeCookie, 2026, 4800, 0, 4800);
    }

    @Test
    void approveReturnsForbiddenWhenActorIsNotApprover() throws Exception {
        Cookie employeeCookie = loginAndGetCookie("huang.yating@example.com", "password123");
        Cookie otherEmployeeCookie = loginAndGetCookie("wu.junxian@example.com", "SalesRep123!");
        Cookie deputyCookie = loginAndGetCookie("li.jianguo@example.com", "FrontendDev123!");

        Long requestId = createRequest(employeeCookie, requestBody(
                "PERSONAL",
                "2026-07-13T09:00:00",
                "2026-07-13T18:00:00",
                "Errand",
                6
        ));

        Long stepId = getPendingStepId(deputyCookie, requestId);

        mockMvc.perform(post("/api/approvals/{stepId}/approve", stepId)
                        .cookie(otherEmployeeCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void approveReturnsBadRequestWhenStepAlreadyProcessed() throws Exception {
        Cookie employeeCookie = loginAndGetCookie("huang.yating@example.com", "password123");
        Cookie deputyCookie = loginAndGetCookie("li.jianguo@example.com", "FrontendDev123!");

        Long requestId = createRequest(employeeCookie, requestBody(
                "SICK",
                "2026-05-25T09:00:00",
                "2026-05-25T18:00:00",
                "Flu",
                6
        ));

        Long stepId = getPendingStepId(deputyCookie, requestId);
        approve(deputyCookie, stepId, "first approval");

        mockMvc.perform(post("/api/approvals/{stepId}/approve", stepId)
                        .cookie(deputyCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    private void approve(Cookie tokenCookie, Long stepId, String comment) throws Exception {
        mockMvc.perform(post("/api/approvals/{stepId}/approve", stepId)
                        .cookie(tokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "comment": "%s"
                                }
                                """.formatted(comment)))
                .andExpect(status().isNoContent());
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

    private Long getPendingStepId(Cookie tokenCookie, Long requestId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/approvals/pending")
                        .cookie(tokenCookie))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = JsonPath.read(result.getResponse().getContentAsString(), "$");

        return items.stream()
                .filter(item -> requestId.longValue() == ((Number) item.get("requestId")).longValue())
                .map(item -> ((Number) item.get("stepId")).longValue())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Pending step not found for request " + requestId));
    }

    private Long getStepIdByIndex(Cookie tokenCookie, Long requestId, int index) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/requests/{id}", requestId)
                        .cookie(tokenCookie))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = JsonPath.read(result.getResponse().getContentAsString(), "$.approvalSteps");
        return ((Number) items.get(index).get("id")).longValue();
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
}
