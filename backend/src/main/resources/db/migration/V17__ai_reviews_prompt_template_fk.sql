-- V17: Link each AI review to the prompt template that produced it.
-- Nullable: RULE_ENGINE synthetic results (low-risk / hard-rule fallback) use no
-- template and leave this NULL. The existing prompt_version column is kept as the
-- human-readable version snapshot alongside the FK. Because templates are immutable
-- (V16), this FK always resolves to the exact text used at review time.

ALTER TABLE ai_reviews
    ADD COLUMN prompt_template_id BIGINT REFERENCES prompt_templates(id);
