package com.eva.workflow.approval.infrastructure.cache;

public final class CacheNames {

    /**
     * Caches the current employee profile returned by GET /api/auth/me.
     * Keyed by employeeId. TTL is intentionally short (30s) to stay consistent
     * with eventual profile changes without requiring explicit eviction.
     */
    public static final String CURRENT_EMPLOYEE = "current-employee";
    public static final String WORKFLOW_RULES = "workflow-rules";
    public static final String HOLIDAY_CALENDAR = "holiday-calendar";
    public static final String EMPLOYEE_SCHEDULE = "employee-schedule";
    public static final String COMPANY_WORK_SCHEDULE = "company-work-schedule";

    private CacheNames() {}
}
