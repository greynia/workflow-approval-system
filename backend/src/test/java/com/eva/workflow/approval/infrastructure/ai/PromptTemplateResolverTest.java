package com.eva.workflow.approval.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.eva.workflow.approval.common.enums.AiProvider;
import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.common.enums.PromptTemplateStatus;
import com.eva.workflow.approval.common.enums.UserRole;
import com.eva.workflow.approval.domain.aireview.model.ReviewSnapshot;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.PromptTemplateEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.PromptTemplateRepository;

class PromptTemplateResolverTest {

    private PromptTemplateRepository repository;
    private PromptTemplateResolver resolver;
    private ReviewSnapshot snapshot;

    @BeforeEach
    void setUp() {
        repository = mock(PromptTemplateRepository.class);
        resolver = new PromptTemplateResolver(repository);
        snapshot = new ReviewSnapshot(
                1L,
                LeaveType.ANNUAL,
                LocalDateTime.of(2026, 5, 1, 9, 0),
                LocalDateTime.of(2026, 5, 1, 17, 0),
                480,
                "vacation",
                Instant.EPOCH,
                2L,
                400L,
                UserRole.EMPLOYEE,
                "Engineering",
                0,
                1
        );
    }

    @Test
    void prefers_model_specific_active_template() {
        PromptTemplateEntity modelSpecific = template(11L, "v-gemini", "model: {{leaveType}}");
        when(repository.findFirstByNameAndLocaleAndProviderAndStatus(
                eq("leave-review"), eq("en"), eq(AiProvider.GEMINI), eq(PromptTemplateStatus.ACTIVE)))
                .thenReturn(Optional.of(modelSpecific));

        RenderedPrompt rendered = resolver.resolveAndRender(snapshot, List.of(), "en", AiProvider.GEMINI);

        assertThat(rendered.templateId()).isEqualTo(11L);
        assertThat(rendered.version()).isEqualTo("v-gemini");
        assertThat(rendered.text()).isEqualTo("model: ANNUAL");
    }

    @Test
    void falls_back_to_shared_template_when_no_model_override() {
        when(repository.findFirstByNameAndLocaleAndProviderAndStatus(
                eq("leave-review"), eq("en"), eq(AiProvider.GEMINI), eq(PromptTemplateStatus.ACTIVE)))
                .thenReturn(Optional.empty());
        PromptTemplateEntity shared = template(22L, "v-shared", "shared: {{leaveType}}");
        when(repository.findFirstByNameAndLocaleAndProviderIsNullAndStatus(
                eq("leave-review"), eq("en"), eq(PromptTemplateStatus.ACTIVE)))
                .thenReturn(Optional.of(shared));

        RenderedPrompt rendered = resolver.resolveAndRender(snapshot, List.of(), "en", AiProvider.GEMINI);

        assertThat(rendered.templateId()).isEqualTo(22L);
        assertThat(rendered.version()).isEqualTo("v-shared");
        assertThat(rendered.text()).isEqualTo("shared: ANNUAL");
    }

    @Test
    void falls_back_to_in_code_default_when_registry_empty() {
        when(repository.findFirstByNameAndLocaleAndProviderAndStatus(
                eq("leave-review"), eq("en"), eq(AiProvider.LOCAL), eq(PromptTemplateStatus.ACTIVE)))
                .thenReturn(Optional.empty());
        when(repository.findFirstByNameAndLocaleAndProviderIsNullAndStatus(
                eq("leave-review"), eq("en"), eq(PromptTemplateStatus.ACTIVE)))
                .thenReturn(Optional.empty());

        RenderedPrompt rendered = resolver.resolveAndRender(snapshot, List.of(), "en", AiProvider.LOCAL);

        assertThat(rendered.templateId()).isNull();
        assertThat(rendered.version()).isEqualTo(AiReviewPromptBuilder.PROMPT_VERSION_HASH);
        assertThat(rendered.text()).isEqualTo(AiReviewPromptBuilder.buildPrompt(snapshot, List.of(), "en"));
    }

    private PromptTemplateEntity template(Long id, String version, String text) {
        PromptTemplateEntity entity = mock(PromptTemplateEntity.class);
        lenient().when(entity.getId()).thenReturn(id);
        lenient().when(entity.getVersion()).thenReturn(version);
        lenient().when(entity.getTemplateText()).thenReturn(text);
        return entity;
    }
}
