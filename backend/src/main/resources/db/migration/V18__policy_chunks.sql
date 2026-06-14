-- V18: Policy RAG store.
-- Company leave-policy clauses, chunked and embedded, for semantic retrieval by the
-- AI-facing /api/ai/policy-search endpoint. Requires the pgvector extension (the DB
-- image is pgvector/pgvector:pg16). Rows are loaded at runtime by PolicyIngestionService
-- because embeddings need an API call and cannot be produced in plain SQL.

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE policy_chunks (
    id              BIGSERIAL    PRIMARY KEY,
    locale          VARCHAR(10)  NOT NULL,          -- 'zh' (en is a follow-up)
    section         VARCHAR(200) NOT NULL,          -- clause heading, used for citation
    content         TEXT         NOT NULL,
    content_hash    VARCHAR(64)  NOT NULL,          -- sha256 of content; ingestion idempotency
    embedding       vector(768)  NOT NULL,          -- text-embedding-004 dimensionality
    embedding_model VARCHAR(60)  NOT NULL,          -- provenance of the vector
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_policy_chunk UNIQUE (locale, content_hash)
);

-- Approximate-nearest-neighbour index for cosine distance (<=>).
CREATE INDEX idx_policy_chunks_embedding
    ON policy_chunks USING hnsw (embedding vector_cosine_ops);
