-- V14: Observability columns for ai_reviews.
-- input_tokens / output_tokens are nullable for historical rows; new rows
-- always populate them (RULE_ENGINE synthetic results use 0, LLM adapters
-- use the provider's reported counts).
-- attempts_json captures per-attempt provenance from FallbackAiReviewAdapter
-- when at least one provider succeeds. All-providers-failed currently falls
-- back to a deterministic RULE_ENGINE result and persists only that attempt;
-- failed provider attempts are not yet persisted (deferred follow-up).
-- Indexes back the admin stats endpoint (group/filter by provider + created_at).

ALTER TABLE ai_reviews
    ADD COLUMN input_tokens   INTEGER,
    ADD COLUMN output_tokens  INTEGER,
    ADD COLUMN is_fallback    BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN attempts_json  JSONB;

CREATE INDEX idx_ai_reviews_provider   ON ai_reviews(provider);
CREATE INDEX idx_ai_reviews_created_at ON ai_reviews(created_at);
