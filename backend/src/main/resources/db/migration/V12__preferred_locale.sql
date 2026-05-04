-- V12: Add preferred_locale to employees for i18n preference persistence.
ALTER TABLE employees
    ADD COLUMN preferred_locale VARCHAR(10) NOT NULL DEFAULT 'zh-TW';
