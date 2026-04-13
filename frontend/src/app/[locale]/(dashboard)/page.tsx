"use client";

import { useTranslations } from "next-intl";
import { useAuthStore } from "@/stores/auth-store";

export default function Home() {
  const t = useTranslations("Home");
  const user = useAuthStore((s) => s.user);

  return (
    <div className="flex min-h-[60vh] items-center justify-center px-4">
      <div className="w-full max-w-sm space-y-4 text-center">
        <h1 className="text-2xl font-bold tracking-tight">{t("Welcome")}</h1>
        {user && (
          <p className="text-sm text-zinc-500">{user.name}</p>
        )}
        <p className="text-sm text-zinc-400">{t("Placeholder")}</p>
      </div>
    </div>
  );
}
