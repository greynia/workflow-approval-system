"use client";

import { useTranslations } from "next-intl";
import { useAuth } from "@/hooks/useAuth";
import { LocaleSwitcher } from "@/components/ui/locale-switcher";

export default function Home() {
  const t = useTranslations("Home");
  const { user, signOut } = useAuth();

  return (
    <div className="relative flex min-h-full items-center justify-center px-4">
      <div className="absolute top-4 right-4">
        <LocaleSwitcher />
      </div>

      <div className="w-full max-w-sm space-y-6 text-center">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">{t("Welcome")}</h1>
          {user && (
            <p className="mt-2 text-sm text-zinc-500">
              {user.name}
            </p>
          )}
          <p className="mt-4 text-sm text-zinc-400">{t("Placeholder")}</p>
        </div>

        <button
          type="button"
          onClick={() => void signOut()}
          className="w-full rounded-md border border-zinc-300 bg-white px-4 py-2 text-sm font-medium text-zinc-900 hover:bg-zinc-50"
        >
          {t("SignOut")}
        </button>
      </div>
    </div>
  );
}
