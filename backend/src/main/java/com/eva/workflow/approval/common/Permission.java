package com.eva.workflow.approval.common;

public final class Permission {
    private Permission() {}

    public static final String REQUEST_VIEW   = "request.view";
    public static final String REQUEST_CREATE = "request.create";
    public static final String REQUEST_EDIT   = "request.edit";
    public static final String REQUEST_DELETE = "request.delete";

    public static final String APPROVAL_VIEW    = "approval.view";
    public static final String APPROVAL_APPROVE = "approval.approve";

    public static final String EMPLOYEE_VIEW = "employee.view";
    public static final String BALANCE_VIEW  = "balance.view";
    public static final String AUDIT_VIEW    = "audit.view";

    public static final String LEAVE_BALANCE_MANAGE = "leave.balance.manage";

    public static final String RECALL_MANAGE = "recall.manage";
}
