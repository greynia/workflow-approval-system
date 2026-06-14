-- V10: Seed employee_schedules
-- All employees use STANDARD schedule (Mon-Fri 09:00-18:00).
-- holidays table is populated via POST /api/admin/holidays/import (ruyut/TaiwanCalendar CDN).
INSERT INTO employee_schedules (employee_id, schedule_type, effective_from) VALUES
    (1, 'STANDARD', '2020-01-06'),
    (2, 'STANDARD', '2020-03-15'),
    (3, 'STANDARD', '2021-06-01'),
    (4, 'STANDARD', '2022-01-03'),
    (5, 'STANDARD', '2022-09-01'),
    (6, 'STANDARD', '2023-07-01'),
    (7, 'STANDARD', '2024-01-02'),
    (8, 'STANDARD', '2024-06-01');

SELECT setval('employee_schedules_id_seq', 8, true);
