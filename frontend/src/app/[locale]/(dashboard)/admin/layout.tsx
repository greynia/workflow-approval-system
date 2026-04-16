"use client";

import { useEffect } from "react";
import { useRouter } from "@/i18n/navigation";
import { useAuthStore } from "@/stores/auth-store";

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const role = useAuthStore((s) => s.user?.role);
  const router = useRouter();

  useEffect(() => {
    if (role && role !== "ADMIN") {
      router.replace("/");
    }
  }, [role, router]);

  if (role === undefined || role === null) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <div className="h-6 w-6 animate-spin rounded-full border-2 border-zinc-300 border-t-zinc-700" />
      </div>
    );
  }

  if (role !== "ADMIN") {
    return null;
  }

  return <>{children}</>;
}
