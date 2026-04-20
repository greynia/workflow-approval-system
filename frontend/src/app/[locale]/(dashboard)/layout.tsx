"use client";

import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { usePathname } from "@/i18n/navigation";
import AuthService from "@/services/auth.service";
import { useAuthStore } from "@/stores/auth-store";
import { useUIStore } from "@/stores/ui-store";
import { Sidebar } from "@/components/layout/sidebar";
import { Header } from "@/components/layout/header";
import { ErrorBoundary } from "@/components/ui/error-boundary";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const user = useAuthStore((s) => s.user);
  const setUser = useAuthStore((s) => s.setUser);
  const closeSidebar = useUIStore((s) => s.closeSidebar);
  const pathname = usePathname();

  const { data: employee } = useQuery({
    queryKey: ["me"],
    queryFn: AuthService.getMe,
    enabled: !user,
    retry: false,
  });

  useEffect(() => {
    if (employee) {
      setUser({ employeeId: employee.id, name: employee.name, role: employee.role, permissions: employee.permissions });
    }
  }, [employee, setUser]);

  useEffect(() => {
    closeSidebar();
  }, [pathname, closeSidebar]);

  return (
    <div className="flex min-h-screen bg-zinc-50">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <Header />
        <main className="flex-1 p-4">
          <ErrorBoundary>{children}</ErrorBoundary>
        </main>
      </div>
    </div>
  );
}
