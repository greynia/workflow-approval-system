-- ============================================================
-- V1: Create Schema
-- Workflow Approval System — Phase 1
-- ============================================================

-- departments
CREATE TABLE departments (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100)        NOT NULL,
    parent_id   BIGINT              REFERENCES departments(id),
    created_at  TIMESTAMP           NOT NULL DEFAULT NOW()
);

-- employees
CREATE TABLE employees (
    id              BIGSERIAL PRIMARY KEY,
    employee_no     VARCHAR(20)         NOT NULL UNIQUE,
    name            VARCHAR(100)        NOT NULL,
    email           VARCHAR(255)        NOT NULL UNIQUE,
    password_hash   VARCHAR(255)        NOT NULL,
    role            VARCHAR(20)         NOT NULL,   -- ADMIN | MANAGER | EMPLOYEE
    department_id   BIGINT              NOT NULL REFERENCES departments(id),
    manager_id      BIGINT              REFERENCES employees(id),
    active          BOOLEAN             NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP           NOT NULL DEFAULT NOW()
);

-- workflow_definitions
CREATE TABLE workflow_definitions (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100)    NOT NULL,
    description TEXT,
    active      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- workflow_rules
CREATE TABLE workflow_rules (
    id                      BIGSERIAL PRIMARY KEY,
    workflow_definition_id  BIGINT          NOT NULL REFERENCES workflow_definitions(id),
    priority                INTEGER         NOT NULL,
    condition_json          JSONB           NOT NULL,   -- e.g. {"maxDays": 3}
    approver_type           VARCHAR(30)     NOT NULL,   -- DIRECT_MANAGER | DEPARTMENT_MANAGER
    CONSTRAINT uq_workflow_rules_definition_priority
        UNIQUE (workflow_definition_id, priority),
    created_at              TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- leave_requests
CREATE TABLE leave_requests (
    id              BIGSERIAL PRIMARY KEY,
    applicant_id    BIGINT          NOT NULL REFERENCES employees(id),
    deputy_id       BIGINT          REFERENCES employees(id),
    type            VARCHAR(20)     NOT NULL,   -- ANNUAL | SICK | PERSONAL | OTHER
    start_date      DATE            NOT NULL,
    end_date        DATE            NOT NULL,
    days            INTEGER         NOT NULL,
    reason          TEXT,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',   -- PENDING | APPROVED | REJECTED | CANCELLED
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- approval_steps
CREATE TABLE approval_steps (
    id              BIGSERIAL PRIMARY KEY,
    leave_request_id BIGINT         NOT NULL REFERENCES leave_requests(id),
    step_order      INTEGER         NOT NULL,
    approver_id     BIGINT          NOT NULL REFERENCES employees(id),
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',   -- PENDING | APPROVED | REJECTED | SKIPPED
    CONSTRAINT uq_approval_steps_request_order
        UNIQUE (leave_request_id, step_order),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- approval_actions
CREATE TABLE approval_actions (
    id              BIGSERIAL PRIMARY KEY,
    approval_step_id BIGINT         NOT NULL REFERENCES approval_steps(id),
    actor_id        BIGINT          NOT NULL REFERENCES employees(id),
    action_type     VARCHAR(20)     NOT NULL,   -- APPROVE | REJECT
    comment         TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- audit_logs
CREATE TABLE audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    entity_type     VARCHAR(50)     NOT NULL,
    entity_id       BIGINT          NOT NULL,
    action          VARCHAR(50)     NOT NULL,
    actor_id        BIGINT          NOT NULL REFERENCES employees(id),
    detail_json     JSONB,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Indexes
-- ============================================================

-- employees
CREATE INDEX idx_employees_department_id  ON employees(department_id);
CREATE INDEX idx_employees_manager_id     ON employees(manager_id);
CREATE INDEX idx_employees_role           ON employees(role);

-- leave_requests
CREATE INDEX idx_leave_requests_applicant_id    ON leave_requests(applicant_id);
CREATE INDEX idx_leave_requests_status          ON leave_requests(status);
CREATE INDEX idx_leave_requests_created_at      ON leave_requests(created_at);

-- approval_steps
CREATE INDEX idx_approval_steps_leave_request_id    ON approval_steps(leave_request_id);
CREATE INDEX idx_approval_steps_approver_id         ON approval_steps(approver_id);
CREATE INDEX idx_approval_steps_status              ON approval_steps(status);
CREATE INDEX idx_approval_steps_approver_status     ON approval_steps(approver_id, status);

-- approval_actions
CREATE INDEX idx_approval_actions_approval_step_id  ON approval_actions(approval_step_id);

-- audit_logs
CREATE INDEX idx_audit_logs_entity_type_id  ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_actor_id        ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_created_at      ON audit_logs(created_at);
