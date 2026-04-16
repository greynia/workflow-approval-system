ALTER TABLE leave_requests
    RENAME COLUMN start_date TO start_time;

ALTER TABLE leave_requests
    ALTER COLUMN start_time TYPE TIMESTAMP USING start_time::timestamp;

ALTER TABLE leave_requests
    RENAME COLUMN end_date TO end_time;

ALTER TABLE leave_requests
    ALTER COLUMN end_time TYPE TIMESTAMP USING end_time::timestamp;

ALTER TABLE leave_requests
    RENAME COLUMN days TO duration_minutes;

UPDATE leave_requests
SET duration_minutes = duration_minutes * 480;

ALTER TABLE approval_steps
    ADD COLUMN step_type VARCHAR(20) NOT NULL DEFAULT 'MANAGER';

UPDATE workflow_rules
SET condition_json = (condition_json - 'maxDays')
    || jsonb_build_object('maxMinutes', (condition_json->>'maxDays')::int * 480)
WHERE condition_json ? 'maxDays';

UPDATE workflow_rules
SET condition_json = (condition_json - 'minDays')
    || jsonb_build_object('minMinutes', (condition_json->>'minDays')::int * 480)
WHERE condition_json ? 'minDays';

CREATE TABLE company_work_schedules (
    id              BIGSERIAL PRIMARY KEY,
    work_start      TIME NOT NULL DEFAULT '09:00',
    work_end        TIME NOT NULL DEFAULT '18:00',
    lunch_start     TIME NOT NULL DEFAULT '12:00',
    lunch_end       TIME NOT NULL DEFAULT '13:00',
    work_days       JSONB NOT NULL DEFAULT '[1,2,3,4,5]',
    effective_from  DATE NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE holidays (
    id          BIGSERIAL PRIMARY KEY,
    date        DATE NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    year        INTEGER NOT NULL
);

CREATE TABLE employee_schedules (
    id              BIGSERIAL PRIMARY KEY,
    employee_id     BIGINT NOT NULL REFERENCES employees(id),
    schedule_type   VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    effective_from  DATE NOT NULL,
    effective_to    DATE,
    detail_json     JSONB
);

INSERT INTO company_work_schedules (id, work_start, work_end, lunch_start, lunch_end, work_days, effective_from)
VALUES (1, '09:00', '18:00', '12:00', '13:00', '[1,2,3,4,5]', DATE '2020-01-01');

SELECT setval('company_work_schedules_id_seq', 1, true);
