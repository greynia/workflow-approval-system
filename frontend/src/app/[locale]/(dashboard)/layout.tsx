"use client";

import { useEffect, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { BarChart3, CheckSquare, FileText, Settings, Workflow } from "lucide-react";
import { Link, usePathname } from "@/i18n/navigation";
import AuthService from "@/services/auth.service";
import ApprovalService from "@/services/approval.service";
import LeaveService from "@/services/leave.service";
import { useAuthStore } from "@/stores/auth-store";
import { useUIStore, useUIStoreHydrated } from "@/stores/ui-store";
import { AppShell } from "@/components/layout/app-shell";
import { AppHeader } from "@/components/layout/app-header";
import { AppSidebar, type SidebarItem } from "@/components/layout/app-sidebar";
import { LocaleSwitcher } from "@/components/ui/locale-switcher";
import { UserMenu } from "@/components/layout/user-menu";
import { ErrorBoundary } from "@/components/ui/error-boundary";
import { UnsavedChangesDialog } from "@/components/ui/unsaved-changes-dialog";
import { useGuardedNavigation } from "@/lib/use-guarded-navigation";
import type { UserRole } from "@/types/auth";

const ROLE_VISIBILITY: Record<string, UserRole[]> = {
  requests: ["EMPLOYEE", "MANAGER", "ADMIN"],
  approvals: ["EMPLOYEE", "MANAGER", "ADMIN"],
  balances: ["EMPLOYEE", "MANAGER", "ADMIN"],
  audit: ["ADMIN"],
};

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const t = useTranslations();
  const tHeader = useTranslations("Header");
  const user = useAuthStore((s) => s.user);
  const setUser = useAuthStore((s) => s.setUser);
  const sidebarOpen = useUIStore((s) => s.sidebarOpen);
  const closeSidebar = useUIStore((s) => s.closeSidebar);
  const toggleSidebar = useUIStore((s) => s.toggleSidebar);
  const sidebarCollapsed = useUIStore((s) => s.sidebarCollapsed);
  const setSidebarCollapsed = useUIStore((s) => s.setSidebarCollapsed);
  const pathname = usePathname();
  const guardedNav = useGuardedNavigation();

  const hasHydrated = useUIStoreHydrated();
  const collapsed = hasHydrated ? sidebarCollapsed : false;

  const { data: employee } = useQuery({
    queryKey: ["me"],
    queryFn: AuthService.getMe,
    enabled: !user,
    retry: false,
  });

  useEffect(() => {
    if (employee) {
      setUser({
        employeeId: employee.id,
        name: employee.name,
        role: employee.role,
        permissions: employee.permissions,
      });
    }
  }, [employee, setUser]);

  useEffect(() => {
    closeSidebar();
  }, [pathname, closeSidebar]);

  const shouldLoadPendingCount = user?.role != null;
  const { data: pendingApprovalCount } = useQuery({
    queryKey: ["approvals", "pending", "count"],
    queryFn: ApprovalService.getPendingCount,
    enabled: shouldLoadPendingCount,
  });
  const { data: pendingRequestCount } = useQuery({
    queryKey: ["requests", "pending", "count"],
    queryFn: LeaveService.getPendingCount,
    enabled: shouldLoadPendingCount,
  });

  const role = user?.role;

  const navItems: SidebarItem[] = useMemo(() => {
    if (!role) return [];
    const all: Array<SidebarItem & { visibility: UserRole[] }> = [
      {
        id: "requests",
        label: t("Sidebar.MyRequests"),
        href: "/requests",
        icon: FileText,
        badge: pendingRequestCount?.count ?? null,
        visibility: ROLE_VISIBILITY.requests,
      },
      {
        id: "approvals",
        label: t("Sidebar.PendingApprovals"),
        href: "/approvals",
        icon: CheckSquare,
        badge: pendingApprovalCount?.count ?? null,
        visibility: ROLE_VISIBILITY.approvals,
      },
      {
        id: "balances",
        label: t("Sidebar.LeaveBalances"),
        href: "/balances",
        icon: BarChart3,
        visibility: ROLE_VISIBILITY.balances,
      },
      {
        id: "audit",
        label: t("Sidebar.AuditLog"),
        href: "/admin/audit-logs",
        icon: Settings,
        visibility: ROLE_VISIBILITY.audit,
      },
    ];
    return all
      .filter((item) => item.visibility.includes(role))
      .map((item): SidebarItem => ({
        id: item.id,
        label: item.label,
        href: item.href,
        icon: item.icon,
        badge: item.badge,
      }));
  }, [role, t, pendingApprovalCount, pendingRequestCount]);

  const brand = (
    <Link
      href="/"
      onClick={(event) => guardedNav.interceptLinkClick("/", event)}
      className="flex items-center gap-2 overflow-hidden rounded-md outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
      aria-label="Workflow home"
    >
      <Workflow className="size-5 shrink-0 text-primary" aria-hidden />
      {!collapsed && (
        <span className="truncate text-sm font-semibold tracking-tight">
          Workflow
        </span>
      )}
    </Link>
  );

  const sidebar = (
    <AppSidebar
      items={navItems}
      open={sidebarOpen}
      onOpenChange={(open) => (open ? toggleSidebar() : closeSidebar())}
      onItemNavigate={(item, event) => guardedNav.interceptLinkClick(item.href, event)}
      brand={brand}
      loading={user === null}
      collapsed={collapsed}
      onCollapsedChange={setSidebarCollapsed}
    />
  );

  const header = (
    <AppHeader
      onToggleSidebar={toggleSidebar}
      toggleLabel={tHeader("OpenSidebar")}
    >
      <LocaleSwitcher />
      <UserMenu />
    </AppHeader>
  );

  return (
    <>
      <AppShell sidebar={sidebar} header={header}>
        <ErrorBoundary>{children}</ErrorBoundary>
      </AppShell>
      <UnsavedChangesDialog />
    </>
  );
}
