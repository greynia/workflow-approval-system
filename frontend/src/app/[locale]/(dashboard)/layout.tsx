"use client";

import { useEffect } from "react";
import { isAxiosError } from "axios";
import { usePathname, useRouter } from "@/i18n/navigation";
import AuthService from "@/services/auth.service";
import { useAuthStore } from "@/stores/auth-store";
import { useUIStore } from "@/stores/ui-store";
import { HTTP_STATUS } from "@/constants/app.constant";
import { appConfig } from "@/configs/app.config";
import { Sidebar } from "@/components/layout/sidebar";
import { Header } from "@/components/layout/header";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const user = useAuthStore((s) => s.user);
  const setUser = useAuthStore((s) => s.setUser);
  const clearUser = useAuthStore((s) => s.clearUser);
  const closeSidebar = useUIStore((s) => s.closeSidebar);
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    if (user) return;
    let cancelled = false;
    AuthService.getMe()
      .then((employee) => {
        if (cancelled) return;
        setUser({
          employeeId: employee.id,
          name: employee.name,
          role: employee.role,
        });
      })
      .catch((err) => {
        if (cancelled) return;
        console.error("[DashboardLayout] getMe failed:", err);
        if (
          isAxiosError(err) &&
          err.response?.status === HTTP_STATUS.UNAUTHORIZED
        ) {
          clearUser();
          router.replace(appConfig.routes.login);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [user, setUser, clearUser, router]);

  useEffect(() => {
    closeSidebar();
  }, [pathname, closeSidebar]);

  return (
    <div className="flex min-h-screen bg-zinc-50">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <Header />
        <main className="flex-1 p-4">{children}</main>
      </div>
    </div>
  );
}
