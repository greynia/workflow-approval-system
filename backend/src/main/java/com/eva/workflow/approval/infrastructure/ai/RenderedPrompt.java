package com.eva.workflow.approval.infrastructure.ai;

/**
 * A fully-rendered prompt plus the provenance of the template it came from.
 *
 * @param text       the prompt sent to the LLM
 * @param templateId the {@code prompt_templates} row used, or {@code null} when the
 *                   in-code default template served as fallback
 * @param version    the human-readable version label (template version, or the
 *                   default template's content hash on the fallback path)
 */
public record RenderedPrompt(String text, Long templateId, String version) {
}
