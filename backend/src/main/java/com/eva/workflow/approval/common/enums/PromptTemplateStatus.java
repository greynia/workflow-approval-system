package com.eva.workflow.approval.common.enums;

/**
 * Lifecycle of a prompt template row. Templates are immutable: editing a prompt
 * means inserting a new row and moving the {@code ACTIVE} flag, so historical
 * {@code ai_reviews} keep pointing at the exact text that produced them.
 */
public enum PromptTemplateStatus {
    DRAFT,
    ACTIVE,
    ARCHIVED
}
