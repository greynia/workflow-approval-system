package com.eva.workflow.approval.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
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
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginSetsHttpOnlyCookieAndReturnsEmployeeSummary() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "huang.yating@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("workflow-token"))
                .andExpect(cookie().httpOnly("workflow-token", true))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.employeeId").value(7))
                .andExpect(jsonPath("$.name").value("黃雅婷"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    void meReturnsCurrentEmployeeWhenJwtCookieIsValid() throws Exception {
        Cookie tokenCookie = loginAndGetCookie("huang.yating@example.com", "password123");

        mockMvc.perform(get("/api/auth/me")
                        .cookie(tokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.employeeNo").value("EMP007"))
                .andExpect(jsonPath("$.name").value("黃雅婷"))
                .andExpect(jsonPath("$.email").value("huang.yating@example.com"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.departmentId").value(5))
                .andExpect(jsonPath("$.managerId").value(5));
    }

    @Test
    void meReturnsCurrentEmployeeWhenAuthorizationHeaderIsValid() throws Exception {
        Cookie tokenCookie = loginAndGetCookie("huang.yating@example.com", "password123");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + tokenCookie.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("黃雅婷"));
    }

    @Test
    void loginRejectsInvalidPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "huang.yating@example.com",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void meRejectsRequestWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutClearsCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("workflow-token"))
                .andExpect(cookie().maxAge("workflow-token", 0));
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
