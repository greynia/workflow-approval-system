"use client";

import { useEffect } from "react";
import { useRouter } from "@/i18n/navigation";
import { useAuthStore } from "@/stores/auth-store";
import { useAuthority } from "@/hooks/useAuthority";

export default function AdminLeaveBalancesLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const user = useAuthStore((s) => s.user);
  const canAccess = useAuthority(user?.permissions ?? [], ["leave.balance_manage"]);
  const router = useRouter();

  useEffect(() => {
    if (user && !canAccess) {
      router.replace("/");
    }
  }, [user, canAccess, router]);

  if (user === null) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <div className="h-6 w-6 animate-spin rounded-full border-2 border-zinc-300 border-t-zinc-700" />
      </div>
    );
  }

  if (!canAccess) {
    return null;
  }

  return <>{children}</>;
}
