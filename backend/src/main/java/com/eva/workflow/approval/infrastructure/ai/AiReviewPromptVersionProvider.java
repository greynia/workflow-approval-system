package com.eva.workflow.approval.infrastructure.ai;

import org.springframework.stereotype.Component;

import com.eva.workflow.approval.application.aireview.AiPromptVersionProvider;

@Component
public class AiReviewPromptVersionProvider implements AiPromptVersionProvider {

    @Override
    public String currentPromptVersion() {
        return AiReviewPromptBuilder.PROMPT_VERSION_HASH;
    }
}
