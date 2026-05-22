package com.eva.workflow.approval.infrastructure.ai;

import org.springframework.stereotype.Component;

import com.eva.workflow.approval.application.aireview.AiPromptVersionProvider;
import com.eva.workflow.approval.common.enums.PromptTemplateStatus;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.PromptTemplateRepository;

@Component
public class AiReviewPromptVersionProvider implements AiPromptVersionProvider {

    private static final String LEAVE_REVIEW = "leave-review";

    private final PromptTemplateRepository promptTemplateRepository;

    public AiReviewPromptVersionProvider(PromptTemplateRepository promptTemplateRepository) {
        this.promptTemplateRepository = promptTemplateRepository;
    }

    /**
     * Reports the active {@code leave-review} template version for the admin stats
     * panel, falling back to the in-code default hash when the registry is empty.
     */
    @Override
    public String currentPromptVersion() {
        return promptTemplateRepository
                .findFirstByNameAndStatusOrderByLocaleAsc(LEAVE_REVIEW, PromptTemplateStatus.ACTIVE)
                .map(template -> template.getVersion())
                .orElse(AiReviewPromptBuilder.PROMPT_VERSION_HASH);
    }
}
