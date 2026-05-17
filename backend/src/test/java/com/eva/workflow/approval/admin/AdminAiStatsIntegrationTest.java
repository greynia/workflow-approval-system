package com.eva.workflow.approval.admin;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.util.ReflectionTestUtils;

import com.eva.workflow.approval.TestcontainersConfiguration;
import com.eva.workflow.approval.common.enums.AiProvider;
import com.eva.workflow.approval.common.enums.AiRecommendation;
import com.eva.workflow.approval.common.enums.AiReviewErrorCode;
import com.eva.workflow.approval.common.enums.AiReviewStatus;
import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.common.enums.RequestStatus;
import com.eva.workflow.approval.common.enums.RiskLevel;
import com.eva.workflow.approval.infrastructure.ai.AiReviewPromptBuilder;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.AiReviewEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.LeaveRequestEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.AiReviewRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.EmployeeRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.LeaveRequestRepository;

import jakarta.servlet.http.Cookie;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AdminAiStatsIntegrationTest {

    private static final long SEED_APPLICANT_ID = 6L;
    private static final String DEGRADED_ERROR_MESSAGE = "gemini quota exceeded";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AiReviewRepository aiReviewRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * Seeded leave requests are inserted straight through the repository (no
     * validation), but other integration tests submit requests via HTTP and run
     * overlap / deputy-availability checks against every existing row. Park the
     * seed rows in a far-future window, one day apart, so they collide with
     * nothing. The stats query filters on the ai_review's createdAt, not these
     * dates, so the window choice does not affect assertions.
     */
    private int seedDayOffset = 0;

    @Test
    void adminGetsAggregatedStatsWithCurrentPromptVersion() throws Exception {
        for (int i = 0; i < 3; i++) {
            seedCompleted(AiProvider.GEMINI, false, 100, 50, 200, singleAttempt());
        }
        seedCompleted(AiProvider.GEMINI, true, 120, 60, 500, degradedAttempts());
        seedCompleted(AiProvider.GEMINI, true, 120, 60, 500, degradedAttempts());
        seedCompleted(AiProvider.RULE_ENGINE, false, 0, 0, 0, singleAttempt());
        seedFailed(AiProvider.GEMINI, AiReviewErrorCode.PROVIDER_UNAVAILABLE);

        Cookie adminCookie = loginAndGetCookie("admin@example.com", "AdminPass123!");

        mockMvc.perform(get("/api/admin/ai/stats").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPromptVersion").value(AiReviewPromptBuilder.PROMPT_VERSION_HASH))
                .andExpect(jsonPath("$.totalReviews").value(greaterThanOrEqualTo(7)))
                .andExpect(jsonPath("$.fallbackCount").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.byProvider.GEMINI.count").value(greaterThanOrEqualTo(6)))
                .andExpect(jsonPath("$.byProvider.GEMINI.completed").value(greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$.byProvider.GEMINI.failed").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.byProvider.GEMINI.totalInputTokens").value(greaterThanOrEqualTo(540)));
    }

    @Test
    void statsAverageLatencyIgnoresNullLatencyRowsWhenCombiningStatusBuckets() throws Exception {
        Instant from = Instant.now();
        seedCompleted(AiProvider.LOCAL, false, 10, 5, 200, singleAttempt());
        seedCompleted(AiProvider.LOCAL, false, 10, 5, null, singleAttempt());
        seedReviewWithStatus(AiProvider.LOCAL, AiReviewStatus.FAILED, 10, 5, 1000, singleAttempt());
        Instant to = Instant.now().plusSeconds(1);

        Cookie adminCookie = loginAndGetCookie("admin@example.com", "AdminPass123!");

        mockMvc.perform(get("/api/admin/ai/stats")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byProvider.LOCAL.count").value(3))
                .andExpect(jsonPath("$.byProvider.LOCAL.avgLatencyMs").value(600.0));
    }

    @Test
    void recentFailuresSplitsHardFailuresAndDegradedReviews() throws Exception {
        for (int i = 0; i < 3; i++) {
            seedCompleted(AiProvider.GEMINI, true, 120, 60, 500, degradedAttempts());
        }
        seedFailed(AiProvider.RULE_ENGINE, AiReviewErrorCode.RULE_ENGINE_FAILED);
        seedFailed(AiProvider.RULE_ENGINE, AiReviewErrorCode.RULE_ENGINE_FAILED);

        Cookie adminCookie = loginAndGetCookie("admin@example.com", "AdminPass123!");

        mockMvc.perform(get("/api/admin/ai/recent-failures").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.degradedReviews[*].isFallback", everyItem(is(true))))
                .andExpect(jsonPath("$.degradedReviews[*].errorMessage", hasItem(DEGRADED_ERROR_MESSAGE)))
                .andExpect(jsonPath("$.hardFailures[*].isFallback", everyItem(is(false))))
                .andExpect(jsonPath("$.hardFailures[*].errorCode", hasItem("RULE_ENGINE_FAILED")));
    }

    @Test
    void recentFailuresToleratesMalformedAttemptsJson() throws Exception {
        AiReviewEntity malformed = seedCompleted(AiProvider.GEMINI, true, 120, 60, 500, "{}");

        Cookie adminCookie = loginAndGetCookie("admin@example.com", "AdminPass123!");

        mockMvc.perform(get("/api/admin/ai/recent-failures")
                        .param("limit", "1")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.degradedReviews[0].reviewId").value(malformed.getId().intValue()))
                .andExpect(jsonPath("$.degradedReviews[0].errorMessage").value(nullValue()));
    }

    @Test
    void recentFailuresClampsLimitToAllowedRange() throws Exception {
        Cookie adminCookie = loginAndGetCookie("admin@example.com", "AdminPass123!");

        mockMvc.perform(get("/api/admin/ai/recent-failures").param("limit", "999").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(50));

        mockMvc.perform(get("/api/admin/ai/recent-failures").param("limit", "0").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(1));

        mockMvc.perform(get("/api/admin/ai/recent-failures").param("limit", "5").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(5))
                .andExpect(jsonPath("$.hardFailures.length()").value(lessThanOrEqualTo(5)))
                .andExpect(jsonPath("$.degradedReviews.length()").value(lessThanOrEqualTo(5)));
    }

    @Test
    void nonAdminCannotAccessAiStatsEndpoints() throws Exception {
        Cookie employeeCookie = loginAndGetCookie("huang.yating@example.com", "password123");

        mockMvc.perform(get("/api/admin/ai/stats").cookie(employeeCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/admin/ai/recent-failures").cookie(employeeCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private AiReviewEntity seedCompleted(
            AiProvider provider,
            boolean isFallback,
            int inputTokens,
            int outputTokens,
            Integer latencyMs,
            String attemptsJson
    ) {
        AiReviewEntity review = AiReviewEntity.createPending(newLeaveRequestId());
        review.markCompleted(
                "Seeded AI review summary",
                RiskLevel.LOW,
                "[]",
                AiRecommendation.APPROVE,
                "Seeded recommendation",
                "[]",
                "{}",
                null,
                "seed-model",
                "seed-hash",
                provider,
                inputTokens,
                outputTokens,
                inputTokens + outputTokens,
                latencyMs,
                isFallback,
                attemptsJson
        );
        return aiReviewRepository.save(review);
    }

    private AiReviewEntity seedReviewWithStatus(
            AiProvider provider,
            AiReviewStatus status,
            int inputTokens,
            int outputTokens,
            Integer latencyMs,
            String attemptsJson
    ) {
        AiReviewEntity review = seedCompleted(provider, false, inputTokens, outputTokens, latencyMs, attemptsJson);
        ReflectionTestUtils.setField(review, "status", status);
        return aiReviewRepository.save(review);
    }

    private void seedFailed(AiProvider provider, AiReviewErrorCode errorCode) {
        AiReviewEntity review = AiReviewEntity.createPending(newLeaveRequestId());
        review.markFailed(errorCode, provider);
        aiReviewRepository.save(review);
    }

    private Long newLeaveRequestId() {
        EmployeeEntity applicant = employeeRepository.findById(SEED_APPLICANT_ID)
                .orElseThrow(() -> new IllegalStateException("Seed applicant not found"));
        LocalDateTime start = LocalDateTime.of(2099, 1, 1, 9, 0).plusDays(seedDayOffset++);
        LeaveRequestEntity request = LeaveRequestEntity.create(
                applicant,
                null,
                LeaveType.ANNUAL,
                start,
                start.plusHours(9),
                480,
                "AI stats seed request",
                RequestStatus.APPROVED
        );
        return leaveRequestRepository.save(request).getId();
    }

    private String singleAttempt() {
        return """
                [{"provider":"GEMINI","modelName":"gemini-2.0","latencyMs":200,"success":true,"errorMessage":null}]
                """;
    }

    private String degradedAttempts() {
        return """
                [{"provider":"GEMINI","modelName":"gemini-2.0","latencyMs":120,"success":false,"errorMessage":"%s"},
                 {"provider":"LOCAL","modelName":"llama3","latencyMs":380,"success":true,"errorMessage":null}]
                """.formatted(DEGRADED_ERROR_MESSAGE);
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
