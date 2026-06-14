package com.eva.workflow.approval.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.eva.workflow.approval.TestcontainersConfiguration;
import com.eva.workflow.approval.application.request.event.LeaveRequestCreatedEvent;
import com.eva.workflow.approval.application.request.event.LeaveRequestCreatedHandler;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

@Import({TestcontainersConfiguration.class, LeaveRequestEventIntegrationTest.TestEventConfig.class})
@SpringBootTest
@AutoConfigureMockMvc
class LeaveRequestEventIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecordingLeaveRequestCreatedHandler eventHandler;

    @BeforeEach
    void setUp() {
        eventHandler.clear();
    }

    @Test
    void createRequestPublishesLeaveRequestCreatedEventAfterCommit() throws Exception {
        Cookie tokenCookie = loginAndGetCookie("huang.yating@example.com", "password123");

        MvcResult result = mockMvc.perform(post("/api/requests")
                        .cookie(tokenCookie)
                        .header("Accept-Language", "zh-TW")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "SICK",
                                  "startTime": "2026-08-03T09:00:00",
                                  "endTime": "2026-08-03T18:00:00",
                                  "reason": "Event test",
                                  "deputyId": 6
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        Number requestId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        LeaveRequestCreatedEvent event = eventHandler.awaitEvent(Duration.ofSeconds(3));

        assertThat(event).isNotNull();
        assertThat(event.leaveRequestId()).isEqualTo(requestId.longValue());
        assertThat(event.applicantId()).isEqualTo(7L);
        assertThat(event.locale()).isEqualTo("zh-TW");
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

    @TestConfiguration
    static class TestEventConfig {

        @Bean
        RecordingLeaveRequestCreatedHandler recordingLeaveRequestCreatedHandler() {
            return new RecordingLeaveRequestCreatedHandler();
        }
    }

    static class RecordingLeaveRequestCreatedHandler implements LeaveRequestCreatedHandler {

        private final LinkedBlockingQueue<LeaveRequestCreatedEvent> events = new LinkedBlockingQueue<>();

        @Override
        public void handle(LeaveRequestCreatedEvent event) {
            events.offer(event);
        }

        LeaveRequestCreatedEvent awaitEvent(Duration timeout) throws InterruptedException {
            return events.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        void clear() {
            events.clear();
        }
    }
}
