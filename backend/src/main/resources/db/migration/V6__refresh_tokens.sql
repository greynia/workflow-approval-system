CREATE TABLE refresh_tokens (
    id                    UUID PRIMARY KEY,
    employee_id           BIGINT      NOT NULL REFERENCES employees(id),
    token_hash            VARCHAR(64) NOT NULL UNIQUE,
    family_id             UUID        NOT NULL,
    expires_at            TIMESTAMP   NOT NULL,
    revoked_at            TIMESTAMP,
    replaced_by_token_id  UUID,
    created_at            TIMESTAMP   NOT NULL DEFAULT NOW(),
    last_used_at          TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_employee_id ON refresh_tokens(employee_id);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
