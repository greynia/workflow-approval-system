package com.eva.workflow.approval.admin;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.eva.workflow.approval.TestcontainersConfiguration;
import com.eva.workflow.approval.common.enums.PromptTemplateStatus;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.PromptTemplateEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.PromptTemplateRepository;

import jakarta.servlet.http.Cookie;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AdminPromptIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PromptTemplateRepository promptTemplateRepository;

    @Test
    void adminListsSeededPromptTemplates() throws Exception {
        Cookie adminCookie = loginAndGetCookie("admin@example.com", "AdminPass123!");

        mockMvc.perform(get("/api/admin/prompts").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(hasItem("leave-review")))
                .andExpect(jsonPath("$[*].locale").value(hasItem("en")))
                .andExpect(jsonPath("$[*].locale").value(hasItem("zh")))
                .andExpect(jsonPath("$[*].version").value(hasItem("v2")))
                .andExpect(jsonPath("$[*].templateText").value(hasItem(
                        org.hamcrest.Matchers.containsString("HR leave request review assistant"))));
    }

    @Test
    void policyAwarePromptMigrationPreservesV1HistoryAndActivatesV2() {
        var templates = promptTemplateRepository.findAllByOrderByNameAscLocaleAscIdDesc(
                PageRequest.of(0, 20));

        assertHasTemplate(templates, "en", "v1", PromptTemplateStatus.ARCHIVED);
        assertHasTemplate(templates, "zh", "v1", PromptTemplateStatus.ARCHIVED);
        assertHasTemplate(templates, "en", "v2", PromptTemplateStatus.ACTIVE);
        assertHasTemplate(templates, "zh", "v2", PromptTemplateStatus.ACTIVE);
    }

    @Test
    void nonAdminIsForbidden() throws Exception {
        Cookie employeeCookie = loginAndGetCookie("huang.yating@example.com", "password123");

        mockMvc.perform(get("/api/admin/prompts").cookie(employeeCookie))
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

    private void assertHasTemplate(
            java.util.List<PromptTemplateEntity> templates,
            String locale,
            String version,
            PromptTemplateStatus status
    ) {
        assertThat(templates)
                .anySatisfy(template -> {
                    assertThat(template.getName()).isEqualTo("leave-review");
                    assertThat(template.getLocale()).isEqualTo(locale);
                    assertThat(template.getVersion()).isEqualTo(version);
                    assertThat(template.getStatus()).isEqualTo(status);
                });
    }
}
