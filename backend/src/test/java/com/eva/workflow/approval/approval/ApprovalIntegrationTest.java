package com.eva.workflow.approval.approval;

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

import com.jayway.jsonpath.JsonPath;
import com.eva.workflow.approval.TestcontainersConfiguration;

import jakarta.servlet.http.Cookie;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ApprovalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pendingListReturnsCurrentApproversPendingSteps() throws Exception {
        Cookie employeeCookie = loginAndGetCookie("huang.yating@example.com", "password123");
        Cookie managerCookie = loginAndGetCookie("zhang.meiling@example.com", "BackendLead123!");

        Long requestId = createRequest(employeeCookie, """
                {
                  "type": "ANNUAL",
                  "startDate": "2026-04-20",
                  "endDate": "2026-04-22",
                  "days": 3,
                  "reason": "Family trip"
                }
                """);

        mockMvc.perform(get("/api/approvals/pending")
                        .cookie(managerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].requestId", hasItem(requestId.intValue())))
                .andExpect(jsonPath("$[*].applicantId", hasItem(7)))
                .andExpect(jsonPath("$[*].leaveType", hasItem("ANNUAL")));
    }

    @Test
    void pendingCountMatchesPendingListSize() throws Exception {
        Cookie employeeCookie = loginAndGetCookie("huang.yating@example.com", "password123");
        Cookie managerCookie = loginAndGetCookie("zhang.meiling@example.com", "BackendLead123!");
        long initialCount = getPendingCount(managerCookie);

        createRequest(employeeCookie, """
                {
                  "type": "PERSONAL",
                  "startDate": "2026-04-25",
                  "endDate": "2026-04-25",
                  "days": 1,
                  "reason": "Errand"
                }
                """);

        mockMvc.perform(get("/api/approvals/pending/count")
                        .cookie(managerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(initialCount + 1));
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

    private long getPendingCount(Cookie tokenCookie) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/approvals/pending/count")
                        .cookie(tokenCookie))
                .andExpect(status().isOk())
                .andReturn();

        Number count = JsonPath.read(result.getResponse().getContentAsString(), "$.count");
        return count.longValue();
    }
}
