-- ============================================================
-- V2: Seed Data
-- 3-level department tree + 8 employees + workflow rules
-- Seed account passwords are documented in docs/01_development_roadmap.md
-- BCrypt cost=10
-- ============================================================

-- ============================================================
-- Departments (3 levels)
-- ============================================================
INSERT INTO departments (id, name, parent_id) VALUES
    (1, '全公司',       NULL),
    (2, '工程部',       1),
    (3, '業務部',       1),
    (4, '前端組',       2),
    (5, '後端組',       2),
    (6, '北區業務組',   3),
    (7, '南區業務組',   3);

SELECT setval('departments_id_seq', 7, true);

-- ============================================================
-- Employees
-- password_hash values match the documented seed account passwords
-- ============================================================
INSERT INTO employees (id, employee_no, name, email, password_hash, role, department_id, manager_id, active) VALUES
    -- Admin
    (1, 'EMP001', '系統管理員', 'admin@example.com',
     '$2y$10$SfoK1I2DqkZ1dq4t8Sknv.cfuegYhb1SuFTtXA47zhuKrHxj/Tov.',
     'ADMIN', 1, NULL, TRUE),

    -- Engineering Manager (工程部主管)
    (2, 'EMP002', '陳大明', 'chen.daming@example.com',
     '$2y$10$pdKRANkm7/SEkP7Hw6FJeueZwmY0hNyMFbQ2i80ONCg8xhv5u.SSG',
     'MANAGER', 2, 1, TRUE),

    -- Sales Manager (業務部主管)
    (3, 'EMP003', '林小華', 'lin.xiaohua@example.com',
     '$2y$10$I83gW7r2x6/4pWeja6luy.gBrh4E2J8JDeHwN3ngBOkjC0V.joxQW',
     'MANAGER', 3, 1, TRUE),

    -- Frontend Team Lead (前端組組長)
    (4, 'EMP004', '王志偉', 'wang.zhiwei@example.com',
     '$2y$10$0ghQWG.CFbbp.YokiC7MWOvo9whoKUaiAsVSrrP86OrSWKgi7fE/O',
     'MANAGER', 4, 2, TRUE),

    -- Backend Team Lead (後端組組長)
    (5, 'EMP005', '張美玲', 'zhang.meiling@example.com',
     '$2y$10$ON6yL1LoUAru6r6Wrahkm.21KOxzzfm51ALzHcNFm0CVBqlJ44T9i',
     'MANAGER', 5, 2, TRUE),

    -- Frontend Developer (一般員工)
    (6, 'EMP006', '李建國', 'li.jianguo@example.com',
     '$2y$10$tWts2Hf9o7pxpGnS7q0CCujImdhEDid9kY4tmBe8MnG4rhuyYl/dy',
     'EMPLOYEE', 4, 4, TRUE),

    -- Backend Developer (一般員工)
    (7, 'EMP007', '黃雅婷', 'huang.yating@example.com',
     '$2y$10$F6wd7byiF/x/tDMGyRaI5eOwoDxOnBGVr4ErZiePCymd.rCcu.jdG',
     'EMPLOYEE', 5, 5, TRUE),

    -- Sales Employee (一般員工)
    (8, 'EMP008', '吳俊賢', 'wu.junxian@example.com',
     '$2y$10$JyxI5Xft5qlUMoQvjRSyUuwqglhlzQ0S3EKRTkotaiD/kdkenONnO',
     'EMPLOYEE', 6, 3, TRUE);

SELECT setval('employees_id_seq', 8, true);

-- ============================================================
-- Workflow Definition
-- ============================================================
INSERT INTO workflow_definitions (id, name, description, active) VALUES
    (1, '標準請假審核', '適用所有員工的標準請假審核流程', TRUE);

SELECT setval('workflow_definitions_id_seq', 1, true);

-- ============================================================
-- Workflow Rules
-- Rule 1: days <= 3 → direct manager only
-- Rule 2: days > 3  → direct manager + department manager
-- ============================================================
INSERT INTO workflow_rules (id, workflow_definition_id, priority, condition_json, approver_type) VALUES
    (1, 1, 1, '{"maxDays": 3}',     'DIRECT_MANAGER'),
    (2, 1, 2, '{"minDays": 4}',     'DIRECT_MANAGER'),
    (3, 1, 3, '{"minDays": 4}',     'DEPARTMENT_MANAGER');

SELECT setval('workflow_rules_id_seq', 3, true);
