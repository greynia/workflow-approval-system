"use client";

import { useTranslations } from "next-intl";
import { useUIStore } from "@/stores/ui-store";
import { LocaleSwitcher } from "@/components/ui/locale-switcher";
import { UserMenu } from "./user-menu";

export function Header() {
  const t = useTranslations("Header");
  const toggleSidebar = useUIStore((s) => s.toggleSidebar);

  return (
    <header className="sticky top-0 z-20 flex h-14 shrink-0 items-center justify-between border-b border-zinc-200 bg-white px-4">
      <button
        type="button"
        onClick={toggleSidebar}
        aria-label={t("OpenSidebar")}
        className="inline-flex h-9 w-9 items-center justify-center rounded-md text-zinc-700 hover:bg-zinc-100 md:hidden"
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="20"
          height="20"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
        >
          <line x1="3" y1="6" x2="21" y2="6" />
          <line x1="3" y1="12" x2="21" y2="12" />
          <line x1="3" y1="18" x2="21" y2="18" />
        </svg>
      </button>

      <div className="flex-1" />

      <div className="flex items-center gap-2">
        <LocaleSwitcher />
        <UserMenu />
      </div>
    </header>
  );
}
