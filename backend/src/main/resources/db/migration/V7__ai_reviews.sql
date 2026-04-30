CREATE TABLE ai_reviews (
    id                    BIGSERIAL    PRIMARY KEY,
    request_id            BIGINT       NOT NULL REFERENCES leave_requests(id) ON DELETE CASCADE,
    status                VARCHAR(20)  NOT NULL,
    summary               TEXT,
    risk_level            VARCHAR(10),
    risk_reasons_json     JSONB,
    recommendation        VARCHAR(30),
    recommendation_reason TEXT,
    hard_rule_flags_json  JSONB,
    input_snapshot_json   JSONB,
    model_name            VARCHAR(80),
    prompt_version        VARCHAR(20),
    provider              VARCHAR(20),
    token_usage           INTEGER,
    latency_ms            INTEGER,
    error_code            VARCHAR(60),
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_ai_review_request UNIQUE (request_id)
);

CREATE INDEX idx_ai_reviews_status ON ai_reviews(status);
