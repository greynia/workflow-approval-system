"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Link, usePathname } from "@/i18n/navigation";
import ApprovalService from "@/services/approval.service";
import { useAuthStore } from "@/stores/auth-store";
import { useUIStore } from "@/stores/ui-store";
import { Skeleton } from "@/components/ui/skeleton";
import { NAV_ITEMS, filterNavItemsByRole } from "./nav-items";

export function Sidebar() {
  const t = useTranslations();
  const user = useAuthStore((s) => s.user);
  const sidebarOpen = useUIStore((s) => s.sidebarOpen);
  const closeSidebar = useUIStore((s) => s.closeSidebar);
  const pathname = usePathname();
  const shouldLoadPendingCount =
    user?.role === "MANAGER" || user?.role === "ADMIN";
  const { data: pendingCount } = useQuery({
    queryKey: ["approvals", "pending-count"],
    queryFn: ApprovalService.getPendingCount,
    enabled: shouldLoadPendingCount,
  });

  const visibleItems = filterNavItemsByRole(NAV_ITEMS, user?.role);

  return (
    <>
      <div
        className={`fixed inset-0 z-30 bg-black/50 md:hidden ${
          sidebarOpen ? "block" : "hidden"
        }`}
        onClick={closeSidebar}
        aria-hidden="true"
      />

      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col border-r border-zinc-200 bg-white transition-transform duration-200 ease-out md:static md:translate-x-0 ${
          sidebarOpen ? "translate-x-0" : "-translate-x-full md:translate-x-0"
        }`}
      >
        <div className="flex h-14 shrink-0 items-center border-b border-zinc-200 px-4">
          <span className="text-sm font-semibold tracking-tight">
            Workflow
          </span>
        </div>

        <nav className="flex-1 space-y-1 overflow-y-auto p-3">
          {user === null ? (
            <>
              <Skeleton className="h-9 w-full" />
              <Skeleton className="h-9 w-full" />
              <Skeleton className="h-9 w-full" />
            </>
          ) : (
            visibleItems.map((item) => {
              const isActive =
                pathname === item.href ||
                pathname.startsWith(`${item.href}/`);
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  onClick={closeSidebar}
                  className={`block rounded px-3 py-2 text-sm font-medium transition-colors ${
                    isActive
                      ? "bg-zinc-900 text-white"
                      : "text-zinc-700 hover:bg-zinc-100"
                  }`}
                >
                  <span>{t(item.labelKey as Parameters<typeof t>[0])}</span>
                  {item.href === "/approvals" && pendingCount?.count ? (
                    <span className="rounded-full bg-white/20 px-2 py-0.5 text-xs text-current">
                      {pendingCount.count}
                    </span>
                  ) : null}
                </Link>
              );
            })
          )}
        </nav>
      </aside>
    </>
  );
}
