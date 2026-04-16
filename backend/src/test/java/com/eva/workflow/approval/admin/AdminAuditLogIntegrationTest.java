package com.eva.workflow.approval.admin;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
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
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AdminAuditLogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminCanQueryAuditLogsWithPaginationAndFilters() throws Exception {
        Cookie adminCookie = loginAndGetCookie("admin@example.com", "AdminPass123!");
        Cookie employeeCookie = loginAndGetCookie("huang.yating@example.com", "password123");
        Cookie deputyCookie = loginAndGetCookie("li.jianguo@example.com", "FrontendDev123!");

        Long requestId = createRequest(employeeCookie, requestBody(
                "SICK",
                "2026-08-03T09:00:00",
                "2026-08-04T18:00:00",
                "Audit log coverage",
                6
        ));

        Long deputyStepId = getPendingStepId(deputyCookie, requestId);
        approve(deputyCookie, deputyStepId, "approved for audit logs");

        mockMvc.perform(get("/api/admin/audit-logs")
                        .cookie(adminCookie)
                        .param("entityType", "LEAVE_REQUEST")
                        .param("action", "APPROVE")
                        .param("actorName", "李建國")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalCount").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.items[*].entityId", hasItem(requestId.intValue())))
                .andExpect(jsonPath("$.items[*].action", everyItem(org.hamcrest.Matchers.is("APPROVE"))))
                .andExpect(jsonPath("$.items[*].actorId", everyItem(org.hamcrest.Matchers.is(6))))
                .andExpect(jsonPath("$.items[*].actorName", everyItem(org.hamcrest.Matchers.is("李建國"))));
    }

    @Test
    void nonAdminCannotQueryAuditLogs() throws Exception {
        Cookie employeeCookie = loginAndGetCookie("huang.yating@example.com", "password123");

        mockMvc.perform(get("/api/admin/audit-logs")
                        .cookie(employeeCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
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

    private Long getPendingStepId(Cookie tokenCookie, Long requestId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/approvals/pending")
                        .cookie(tokenCookie))
                .andExpect(status().isOk())
                .andReturn();

        java.util.List<java.util.Map<String, Object>> items =
                JsonPath.read(result.getResponse().getContentAsString(), "$[?(@.requestId == %s)]".formatted(requestId));

        Number stepId = (Number) items.get(0).get("stepId");
        return stepId.longValue();
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
