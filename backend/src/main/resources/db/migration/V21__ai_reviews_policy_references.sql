-- V21: store the company-policy clauses an AI review cited.
-- JSONB array of {section, source, chunkIndex, content, score}, mirroring the other ai_reviews
-- JSON columns. Populated by the orchestrator when policy retrieval ran; surfaced to approvers.

ALTER TABLE ai_reviews ADD COLUMN policy_references_json JSONB;
