package com.eva.workflow.approval.infrastructure.ai;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.eva.workflow.approval.common.enums.AiProvider;
import com.eva.workflow.approval.common.enums.PromptTemplateStatus;
import com.eva.workflow.approval.domain.aireview.model.HardRuleFlag;
import com.eva.workflow.approval.domain.aireview.model.ReviewSnapshot;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.PromptTemplateEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.PromptTemplateRepository;

/**
 * Picks the active prompt template for a given model + locale and renders it.
 *
 * <p>Resolution is most-specific-wins: a {@code (name, locale, provider)} ACTIVE row
 * (model-specific override) beats the shared {@code provider IS NULL} ACTIVE row.
 * If neither exists the in-code default ({@link AiReviewPromptBuilder}) is used, so a
 * missing or unseeded registry degrades gracefully instead of breaking review.
 *
 * <p>Selection happens here — inside the adapter path — rather than in the orchestrator,
 * because the {@code FallbackAiReviewAdapter} may switch models mid-request; each model
 * must resolve its own template at the moment it runs.
 */
@Component
public class PromptTemplateResolver {

    private static final Logger log = LoggerFactory.getLogger(PromptTemplateResolver.class);
    private static final String LEAVE_REVIEW = "leave-review";

    private final PromptTemplateRepository repository;

    public PromptTemplateResolver(PromptTemplateRepository repository) {
        this.repository = repository;
    }

    public RenderedPrompt resolveAndRender(
            ReviewSnapshot snapshot, List<HardRuleFlag> flags, String locale, AiProvider provider,
            String policyContext) {
        String normalizedLocale = normalizeLocale(locale);

        PromptTemplateEntity template = repository
                .findFirstByNameAndLocaleAndProviderAndStatus(
                        LEAVE_REVIEW, normalizedLocale, provider, PromptTemplateStatus.ACTIVE)
                .or(() -> repository.findFirstByNameAndLocaleAndProviderIsNullAndStatus(
                        LEAVE_REVIEW, normalizedLocale, PromptTemplateStatus.ACTIVE))
                .orElse(null);

        if (template == null) {
            log.warn("No active prompt template for name={} locale={} provider={}; using in-code default",
                    LEAVE_REVIEW, normalizedLocale, provider);
            String text = AiReviewPromptBuilder.buildPrompt(snapshot, flags, locale, policyContext);
            return new RenderedPrompt(text, null, AiReviewPromptBuilder.PROMPT_VERSION_HASH);
        }

        String text = AiReviewPromptBuilder.render(
                template.getTemplateText(), snapshot, flags, locale, policyContext);
        return new RenderedPrompt(text, template.getId(), template.getVersion());
    }

    private static String normalizeLocale(String locale) {
        return locale != null && locale.toLowerCase().startsWith("zh") ? "zh" : "en";
    }
}
