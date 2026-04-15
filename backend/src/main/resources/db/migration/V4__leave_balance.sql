-- ============================================================
-- V4: Leave Balance
-- Adds hire_date to employees and introduces per-employee,
-- per-year, per-type leave balance tracking.
--
-- Quota calculation basis (勞基法):
--   ANNUAL  — seniority-based (see LeaveBalanceService)
--   SICK    — 30 working days (14400 min) per year
--   PERSONAL— 7 working days (3360 min) per year
--
-- Seed quotas below are based on each employee's anniversary
-- that falls within 2026 (i.e. completed years at 2026 anniversary).
-- ============================================================

ALTER TABLE employees ADD COLUMN hire_date DATE;

UPDATE employees SET hire_date = '2020-01-06' WHERE id = 1;  -- 系統管理員
UPDATE employees SET hire_date = '2020-03-15' WHERE id = 2;  -- 陳大明  (2026 ann: 6 yrs → 15d)
UPDATE employees SET hire_date = '2021-06-01' WHERE id = 3;  -- 林小華  (2026 ann: 5 yrs → 15d)
UPDATE employees SET hire_date = '2022-01-03' WHERE id = 4;  -- 王志偉  (2026 ann: 4 yrs → 14d)
UPDATE employees SET hire_date = '2022-09-01' WHERE id = 5;  -- 張美玲  (2026 ann: 4 yrs → 14d)
UPDATE employees SET hire_date = '2023-07-01' WHERE id = 6;  -- 李建國  (2026 ann: 3 yrs → 14d)
UPDATE employees SET hire_date = '2024-01-02' WHERE id = 7;  -- 黃雅婷  (2026 ann: 2 yrs → 10d)
UPDATE employees SET hire_date = '2024-06-01' WHERE id = 8;  -- 吳俊賢  (2026 ann: 2 yrs → 10d)

ALTER TABLE employees ALTER COLUMN hire_date SET NOT NULL;

-- ============================================================
-- Leave balance table
-- ============================================================
CREATE TABLE employee_leave_balances (
    id              BIGSERIAL PRIMARY KEY,
    employee_id     BIGINT       NOT NULL REFERENCES employees(id),
    year            INTEGER      NOT NULL,
    leave_type      VARCHAR(20)  NOT NULL,   -- ANNUAL | SICK | PERSONAL
    quota_minutes   INTEGER      NOT NULL DEFAULT 0,
    used_minutes    INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_employee_leave_balance UNIQUE (employee_id, year, leave_type),
    CONSTRAINT chk_used_not_negative CHECK (used_minutes >= 0)
);

CREATE INDEX idx_leave_balances_employee_year
    ON employee_leave_balances(employee_id, year);

-- ============================================================
-- Seed 2026 balances for all existing employees
-- ANNUAL minutes = days × 480  (1 working day = 8h = 480 min)
-- SICK    = 30d × 480 = 14400
-- PERSONAL = 7d × 480 =  3360
-- ============================================================
INSERT INTO employee_leave_balances (employee_id, year, leave_type, quota_minutes) VALUES
    -- EMP001 系統管理員  6 yrs → 15d annual
    (1, 2026, 'ANNUAL',   7200),
    (1, 2026, 'SICK',    14400),
    (1, 2026, 'PERSONAL', 3360),
    -- EMP002 陳大明  6 yrs → 15d
    (2, 2026, 'ANNUAL',   7200),
    (2, 2026, 'SICK',    14400),
    (2, 2026, 'PERSONAL', 3360),
    -- EMP003 林小華  5 yrs → 15d
    (3, 2026, 'ANNUAL',   7200),
    (3, 2026, 'SICK',    14400),
    (3, 2026, 'PERSONAL', 3360),
    -- EMP004 王志偉  4 yrs → 14d
    (4, 2026, 'ANNUAL',   6720),
    (4, 2026, 'SICK',    14400),
    (4, 2026, 'PERSONAL', 3360),
    -- EMP005 張美玲  4 yrs → 14d
    (5, 2026, 'ANNUAL',   6720),
    (5, 2026, 'SICK',    14400),
    (5, 2026, 'PERSONAL', 3360),
    -- EMP006 李建國  3 yrs → 14d
    (6, 2026, 'ANNUAL',   6720),
    (6, 2026, 'SICK',    14400),
    (6, 2026, 'PERSONAL', 3360),
    -- EMP007 黃雅婷  2 yrs → 10d
    (7, 2026, 'ANNUAL',   4800),
    (7, 2026, 'SICK',    14400),
    (7, 2026, 'PERSONAL', 3360),
    -- EMP008 吳俊賢  2 yrs → 10d
    (8, 2026, 'ANNUAL',   4800),
    (8, 2026, 'SICK',    14400),
    (8, 2026, 'PERSONAL', 3360);

SELECT setval('employee_leave_balances_id_seq', 24, true);
