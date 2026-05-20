package com.eva.workflow.approval.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.eva.workflow.approval.TestcontainersConfiguration;
import com.eva.workflow.approval.common.enums.ApprovalStepType;
import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.common.enums.RequestStatus;
import com.eva.workflow.approval.common.enums.StepStatus;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.ApprovalStepEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.AuditLogEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.LeaveRequestEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.ApprovalStepRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.AuditLogRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.EmployeeRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.LeaveRequestRepository;

import jakarta.servlet.http.Cookie;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AiContextControllerIntegrationTest {

    private static final long REGULAR_APPLICANT_ID = 6L;   // 李建國 — tenure > 90d
    private static final long NEW_HIRE_APPLICANT_ID = 9L;  // 周怡君 — hire_date = today - 30d
    private static final long MANAGER_APPROVER_ID = 4L;    // 王志偉 — MANAGER

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private ApprovalStepRepository approvalStepRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private int seedDayOffset = 0;

    @Test
    void aiAgentReadsContextWithApprovalFlow() throws Exception {
        Long requestId = seedRequestWithApprovalStep(REGULAR_APPLICANT_ID);
        Cookie cookie = loginAndGetCookie("ai-agent@system.local", "password123");

        mockMvc.perform(get("/api/ai/requests/{id}/context", requestId).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request.id").value(requestId.intValue()))
                .andExpect(jsonPath("$.request.leaveType").value("ANNUAL"))
                .andExpect(jsonPath("$.applicant.id").value((int) REGULAR_APPLICANT_ID))
                .andExpect(jsonPath("$.applicant.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.applicant.departmentName").exists())
                .andExpect(jsonPath("$.applicant.tenureDays").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.stats.recentLeaveCountLast30Days").exists())
                .andExpect(jsonPath("$.approvalFlow.length()").value(1))
                .andExpect(jsonPath("$.approvalFlow[0].stepType").value("MANAGER"))
                .andExpect(jsonPath("$.approvalFlow[0].status").value("PENDING"))
                .andExpect(jsonPath("$.approvalFlow[0].approverRole").value("MANAGER"));

        List<AuditLogEntity> logs = auditLogRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc("AI_CONTEXT", requestId);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getAction()).isEqualTo("READ");
    }

    @Test
    void aiAgentEvaluatesRulesForNewHireLongLeave() throws Exception {
        Long requestId = seedLongLeaveRequest(NEW_HIRE_APPLICANT_ID);
        Cookie cookie = loginAndGetCookie("ai-agent@system.local", "password123");

        mockMvc.perform(get("/api/ai/requests/{id}/rule-evaluation", requestId).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(requestId.intValue()))
                .andExpect(jsonPath("$.highestRiskLevel").value("HIGH"))
                .andExpect(jsonPath("$.flags[*].code").value(org.hamcrest.Matchers.hasItem("NEW_HIRE_LONG_LEAVE")))
                .andExpect(jsonPath("$.flags[?(@.code == 'NEW_HIRE_LONG_LEAVE')].humanReadable")
                        .value(org.hamcrest.Matchers.hasItem(containsString("New hire"))));

        List<AuditLogEntity> logs = auditLogRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc("AI_RULE_EVAL", requestId);
        assertThat(logs).hasSize(1);
    }

    @Test
    void ruleEvaluationLocaleZhReturnsChineseMessage() throws Exception {
        Long requestId = seedLongLeaveRequest(NEW_HIRE_APPLICANT_ID);
        Cookie cookie = loginAndGetCookie("ai-agent@system.local", "password123");

        mockMvc.perform(get("/api/ai/requests/{id}/rule-evaluation", requestId)
                        .param("locale", "zh")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flags[?(@.code == 'NEW_HIRE_LONG_LEAVE')].humanReadable")
                        .value(org.hamcrest.Matchers.hasItem(containsString("新進員工"))));
    }

    @Test
    void adminCanAlsoReadContextAndRuleEvaluation() throws Exception {
        Long requestId = seedRequestWithApprovalStep(REGULAR_APPLICANT_ID);
        Cookie cookie = loginAndGetCookie("admin@example.com", "AdminPass123!");

        mockMvc.perform(get("/api/ai/requests/{id}/context", requestId).cookie(cookie))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/ai/requests/{id}/rule-evaluation", requestId).cookie(cookie))
                .andExpect(status().isOk());
    }

    @Test
    void regularEmployeeCannotAccessAiEndpoints() throws Exception {
        Long requestId = seedRequestWithApprovalStep(REGULAR_APPLICANT_ID);
        Cookie cookie = loginAndGetCookie("huang.yating@example.com", "password123");

        mockMvc.perform(get("/api/ai/requests/{id}/context", requestId).cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(get("/api/ai/requests/{id}/rule-evaluation", requestId).cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void missingRequestReturnsNotFound() throws Exception {
        Cookie cookie = loginAndGetCookie("ai-agent@system.local", "password123");

        mockMvc.perform(get("/api/ai/requests/{id}/context", 999_999L).cookie(cookie))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/ai/requests/{id}/rule-evaluation", 999_999L).cookie(cookie))
                .andExpect(status().isNotFound());
    }

    private Long seedRequestWithApprovalStep(long applicantId) {
        EmployeeEntity applicant = employeeRepository.findById(applicantId)
                .orElseThrow(() -> new IllegalStateException("Seed applicant not found: " + applicantId));
        EmployeeEntity approver = employeeRepository.findById(MANAGER_APPROVER_ID)
                .orElseThrow(() -> new IllegalStateException("Seed manager not found"));

        LocalDateTime start = LocalDateTime.of(2099, 1, 1, 9, 0).plusDays(seedDayOffset++);
        LeaveRequestEntity request = LeaveRequestEntity.create(
                applicant, null, LeaveType.ANNUAL, start, start.plusHours(8), 480,
                "AI context IT seed", RequestStatus.PENDING
        );
        LeaveRequestEntity saved = leaveRequestRepository.save(request);

        approvalStepRepository.save(ApprovalStepEntity.create(
                saved, 1, approver, ApprovalStepType.MANAGER, StepStatus.PENDING
        ));
        return saved.getId();
    }

    private Long seedLongLeaveRequest(long applicantId) {
        EmployeeEntity applicant = employeeRepository.findById(applicantId)
                .orElseThrow(() -> new IllegalStateException("Seed applicant not found: " + applicantId));

        LocalDateTime start = LocalDateTime.of(2099, 6, 1, 9, 0).plusDays(seedDayOffset++);
        LeaveRequestEntity request = LeaveRequestEntity.create(
                applicant, null, LeaveType.PERSONAL, start, start.plusDays(4), 2880,
                "AI rule eval IT seed", RequestStatus.PENDING
        );
        return leaveRequestRepository.save(request).getId();
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
