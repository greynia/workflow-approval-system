package com.eva.workflow.approval.request;

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
class LeaveRequestIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createRequestPersistsPendingLeaveRequest() throws Exception {
        Cookie tokenCookie = loginAndGetCookie("huang.yating@example.com", "password123");

        mockMvc.perform(post("/api/requests")
                        .cookie(tokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "ANNUAL",
                                  "startDate": "2026-04-20",
                                  "endDate": "2026-04-22",
                                  "days": 3,
                                  "reason": "Family trip",
                                  "deputyId": 6
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.applicantId").value(7))
                .andExpect(jsonPath("$.applicantName").value("黃雅婷"))
                .andExpect(jsonPath("$.deputyId").value(6))
                .andExpect(jsonPath("$.type").value("ANNUAL"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.approvalSteps").isArray())
                .andExpect(jsonPath("$.approvalSteps").isEmpty())
                .andExpect(jsonPath("$.approvalActions").isArray())
                .andExpect(jsonPath("$.approvalActions").isEmpty());
    }

    @Test
    void listReturnsOnlyCurrentApplicantsRequestsWithPagination() throws Exception {
        Cookie tokenCookie = loginAndGetCookie("huang.yating@example.com", "password123");

        createRequest(tokenCookie, """
                {
                  "type": "SICK",
                  "startDate": "2026-04-24",
                  "endDate": "2026-04-24",
                  "days": 1,
                  "reason": "Flu"
                }
                """);

        createRequest(tokenCookie, """
                {
                  "type": "PERSONAL",
                  "startDate": "2026-04-28",
                  "endDate": "2026-04-29",
                  "days": 2,
                  "reason": "Personal errand"
                }
                """);

        mockMvc.perform(get("/api/requests")
                        .cookie(tokenCookie)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].status").value("PENDING"))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void detailReturnsCreatedRequestForApplicantOnly() throws Exception {
        Cookie tokenCookie = loginAndGetCookie("huang.yating@example.com", "password123");

        Long requestId = createRequest(tokenCookie, """
                {
                  "type": "OTHER",
                  "startDate": "2026-05-01",
                  "endDate": "2026-05-01",
                  "days": 1,
                  "reason": "Administrative leave"
                }
                """);

        mockMvc.perform(get("/api/requests/{id}", requestId)
                        .cookie(tokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requestId))
                .andExpect(jsonPath("$.applicantId").value(7))
                .andExpect(jsonPath("$.reason").value("Administrative leave"))
                .andExpect(jsonPath("$.approvalSteps").isEmpty())
                .andExpect(jsonPath("$.approvalActions").isEmpty());
    }

    @Test
    void detailRejectsAccessToAnotherEmployeesRequest() throws Exception {
        Cookie employee7Cookie = loginAndGetCookie("huang.yating@example.com", "password123");
        Cookie employee6Cookie = loginAndGetCookie("li.jianguo@example.com", "FrontendDev123!");

        Long requestId = createRequest(employee7Cookie, """
                {
                  "type": "ANNUAL",
                  "startDate": "2026-05-03",
                  "endDate": "2026-05-03",
                  "days": 1,
                  "reason": "Private schedule"
                }
                """);

        mockMvc.perform(get("/api/requests/{id}", requestId)
                        .cookie(employee6Cookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void createRejectsDeputySameAsApplicant() throws Exception {
        Cookie tokenCookie = loginAndGetCookie("huang.yating@example.com", "password123");

        mockMvc.perform(post("/api/requests")
                        .cookie(tokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "ANNUAL",
                                  "startDate": "2026-04-20",
                                  "endDate": "2026-04-20",
                                  "days": 1,
                                  "reason": "One day off",
                                  "deputyId": 7
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void listRejectsInvalidPaginationArguments() throws Exception {
        Cookie tokenCookie = loginAndGetCookie("huang.yating@example.com", "password123");

        mockMvc.perform(get("/api/requests")
                        .cookie(tokenCookie)
                        .param("page", "-1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    private Long createRequest(Cookie tokenCookie, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/requests")
                        .cookie(tokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        int idStart = json.indexOf("\"id\":") + 5;
        int idEnd = json.indexOf(",", idStart);
        return Long.parseLong(json.substring(idStart, idEnd));
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
