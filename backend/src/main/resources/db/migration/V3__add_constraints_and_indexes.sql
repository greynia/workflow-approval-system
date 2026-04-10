-- ============================================================
-- V3: Add Missing Constraints and Indexes
-- ============================================================

-- Prevent duplicate step_order within the same leave request
ALTER TABLE approval_steps
    ADD CONSTRAINT uq_approval_steps_request_order
    UNIQUE (leave_request_id, step_order);

-- Prevent duplicate priority within the same workflow definition.
-- PostgreSQL creates a unique index internally for UNIQUE constraints,
-- so the regular index from V1 (idx_workflow_rules_definition_id) becomes redundant.
DROP INDEX IF EXISTS idx_workflow_rules_definition_id;
ALTER TABLE workflow_rules
    ADD CONSTRAINT uq_workflow_rules_definition_priority
    UNIQUE (workflow_definition_id, priority);

-- Composite index for the common query: pending steps for a given approver
CREATE INDEX idx_approval_steps_approver_status
    ON approval_steps(approver_id, status);
