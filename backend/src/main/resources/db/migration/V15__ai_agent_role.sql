-- ============================================================
-- V15: AI Agent service-account role
--
-- Seeds a single non-human service account used by the
-- enterprise-ai-skill-harness to call AI-facing endpoints
-- (/api/ai/requests/{id}/context, /rule-evaluation).
--
-- The `role` column is plain VARCHAR(20) with no CHECK
-- constraint, so the new AI_AGENT value needs no DDL.
-- Password hash is shared with the demo employee seed and
-- is documented in docs/01_development_roadmap.md.
--
-- Idempotent + sequence-safe: lets BIGSERIAL pick the id
-- and skips the insert if the account already exists.
-- ============================================================

INSERT INTO employees (
    employee_no,
    name,
    email,
    password_hash,
    role,
    department_id,
    manager_id,
    hire_date,
    active,
    preferred_locale
) VALUES (
    'EMP-AI-AGENT',
    'AI Agent',
    'ai-agent@system.local',
    '$2y$10$F6wd7byiF/x/tDMGyRaI5eOwoDxOnBGVr4ErZiePCymd.rCcu.jdG',
    'AI_AGENT',
    1,
    NULL,
    CURRENT_DATE,
    TRUE,
    'en'
)
ON CONFLICT (email) DO NOTHING;
