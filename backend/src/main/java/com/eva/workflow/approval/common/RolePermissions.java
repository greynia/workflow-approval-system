package com.eva.workflow.approval.common;

import com.eva.workflow.approval.common.enums.UserRole;
import java.util.List;
import java.util.Map;

public final class RolePermissions {
    private RolePermissions() {}

    private static final Map<UserRole, List<String>> MAP = Map.of(
        UserRole.ADMIN, List.of(
            Permission.REQUEST_VIEW, Permission.REQUEST_CREATE, Permission.REQUEST_EDIT, Permission.REQUEST_DELETE,
            Permission.APPROVAL_VIEW, Permission.APPROVAL_APPROVE,
            Permission.EMPLOYEE_VIEW, Permission.BALANCE_VIEW, Permission.AUDIT_VIEW,
            Permission.LEAVE_BALANCE_MANAGE
        ),
        UserRole.MANAGER, List.of(
            Permission.REQUEST_VIEW, Permission.REQUEST_CREATE, Permission.REQUEST_EDIT,
            Permission.APPROVAL_VIEW, Permission.APPROVAL_APPROVE,
            Permission.EMPLOYEE_VIEW, Permission.BALANCE_VIEW
        ),
        UserRole.EMPLOYEE, List.of(
            Permission.REQUEST_VIEW, Permission.REQUEST_CREATE, Permission.REQUEST_EDIT,
            Permission.EMPLOYEE_VIEW, Permission.BALANCE_VIEW
        )
    );

    public static List<String> of(UserRole role) {
        return MAP.getOrDefault(role, List.of());
    }
}
