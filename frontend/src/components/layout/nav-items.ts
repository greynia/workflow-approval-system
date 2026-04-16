import type { UserRole } from "@/types/auth";

export type NavItem = {
  labelKey: string;
  href: string;
  roles: UserRole[];
};

export const NAV_ITEMS: NavItem[] = [
  {
    labelKey: "Sidebar.MyRequests",
    href: "/requests",
    roles: ["EMPLOYEE", "MANAGER", "ADMIN"],
  },
  {
    labelKey: "Sidebar.PendingApprovals",
    href: "/approvals",
    roles: ["EMPLOYEE", "MANAGER", "ADMIN"],
  },
  {
    labelKey: "Sidebar.LeaveBalances",
    href: "/balances",
    roles: ["EMPLOYEE", "MANAGER", "ADMIN"],
  },
  {
    labelKey: "Sidebar.AuditLog",
    href: "/admin/audit-logs",
    roles: ["ADMIN"],
  },
];

/**
 * Filter nav items based on the current user's role.
 * If role is null/undefined (not yet loaded), return an empty list.
 */
export function filterNavItemsByRole(
  items: NavItem[],
  role: UserRole | null | undefined,
): NavItem[] {
  if (!role) return [];
  return items.filter((item) => item.roles.includes(role));
}
