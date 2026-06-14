package com.eva.workflow.approval.policy;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
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

/**
 * Exercises the full RAG path against a real pgvector container: the startup
 * ingestion populates policy_chunks, and the endpoint embeds the query (via the
 * deterministic test embedder) and runs a cosine search. ADMIN stands in for the
 * AI_AGENT role here — both pass the same service-layer guard.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PolicySearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void privilegedCallerGetsRerankedPolicyMatches() throws Exception {
        Cookie adminCookie = loginAndGetCookie("admin@example.com", "AdminPass123!");

        mockMvc.perform(get("/api/ai/policy-search")
                        .param("query", "新進員工請假限制與高風險審核")
                        .param("topK", "3")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topK").value(3))
                .andExpect(jsonPath("$.reranked").value(true))
                .andExpect(jsonPath("$.matches.length()").value(lessThanOrEqualTo(3)))
                .andExpect(jsonPath("$.matches.length()").value(greaterThanOrEqualTo(1)))
                // Highest character-overlap with the query → reranked first.
                .andExpect(jsonPath("$.matches[0].section").value("新進員工請假限制"))
                .andExpect(jsonPath("$.matches[0].source").value("leave-policy.zh.md"))
                .andExpect(jsonPath("$.matches[0].chunkIndex").value(0))
                .andExpect(jsonPath("$.matches[0].score").value(greaterThanOrEqualTo(0.5)));
    }

    @Test
    void topKIsClampedToFinalTopK() throws Exception {
        Cookie adminCookie = loginAndGetCookie("admin@example.com", "AdminPass123!");

        mockMvc.perform(get("/api/ai/policy-search")
                        .param("query", "請假")
                        .param("topK", "999")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topK").value(5))
                .andExpect(jsonPath("$.matches.length()").value(lessThanOrEqualTo(5)));
    }

    @Test
    void irrelevantQueryYieldsNoMatchesAfterMinScore() throws Exception {
        Cookie adminCookie = loginAndGetCookie("admin@example.com", "AdminPass123!");

        mockMvc.perform(get("/api/ai/policy-search")
                        .param("query", "披薩義大利麵漢堡薯條可樂")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reranked").value(true))
                .andExpect(jsonPath("$.matches.length()").value(0));
    }

    @Test
    void blankQueryIsRejected() throws Exception {
        Cookie adminCookie = loginAndGetCookie("admin@example.com", "AdminPass123!");

        mockMvc.perform(get("/api/ai/policy-search")
                        .param("query", "  ")
                        .cookie(adminCookie))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonPrivilegedCallerIsForbidden() throws Exception {
        Cookie employeeCookie = loginAndGetCookie("huang.yating@example.com", "password123");

        mockMvc.perform(get("/api/ai/policy-search")
                        .param("query", "請假")
                        .cookie(employeeCookie))
                .andExpect(status().isForbidden());
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
