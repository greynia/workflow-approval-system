-- ============================================================
-- V8: Phase 2A AI Review Demo Seed
-- Adds a new-hire employee account for deterministic local/demo
-- testing of AI hard-rule risk levels.
-- ============================================================

INSERT INTO employees (
    id,
    employee_no,
    name,
    email,
    password_hash,
    role,
    department_id,
    manager_id,
    hire_date,
    active
) VALUES (
    9,
    'EMP009',
    '周怡君',
    'zhou.yijun@example.com',
    '$2y$10$F6wd7byiF/x/tDMGyRaI5eOwoDxOnBGVr4ErZiePCymd.rCcu.jdG',
    'EMPLOYEE',
    5,
    5,
    CURRENT_DATE - 30,
    TRUE
);

SELECT setval('employees_id_seq', 9, true);

INSERT INTO employee_leave_balances (employee_id, year, leave_type, quota_minutes) VALUES
    (9, 2026, 'ANNUAL',      0),
    (9, 2026, 'SICK',    14400),
    (9, 2026, 'PERSONAL', 3360);

SELECT setval('employee_leave_balances_id_seq', 27, true);
