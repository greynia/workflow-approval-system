"use client";

import * as DropdownMenu from "@radix-ui/react-dropdown-menu";
import { useTransition } from "react";
import { useLocale } from "next-intl";
import { usePathname } from "@/i18n/navigation";
import { useGuardedNavigation } from "@/lib/use-guarded-navigation";

const localeOptions = [
  { value: "zh-TW", shortLabel: "繁中", label: "繁體中文" },
  { value: "en", shortLabel: "EN", label: "English" },
] as const;

export function LocaleSwitcher() {
  const currentLocale = useLocale();
  const guardedNav = useGuardedNavigation();
  const pathname = usePathname();
  const [isPending, startTransition] = useTransition();

  const currentOption =
    localeOptions.find((option) => option.value === currentLocale) ??
    localeOptions[0];

  function handleSwitch(nextLocale: (typeof localeOptions)[number]["value"]) {
    if (nextLocale === currentLocale) return;

    startTransition(() => {
      guardedNav.replace({ pathname }, { locale: nextLocale });
    });
  }

  return (
    <DropdownMenu.Root>
      <DropdownMenu.Trigger asChild>
        <button
          type="button"
          disabled={isPending}
          className="inline-flex items-center gap-2 rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium text-zinc-700 transition-colors hover:bg-zinc-100 disabled:cursor-wait disabled:opacity-70"
        >
          <span>{currentOption.shortLabel}</span>
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
          >
            <path d="m6 9 6 6 6-6" />
          </svg>
        </button>
      </DropdownMenu.Trigger>

      <DropdownMenu.Portal>
        <DropdownMenu.Content
          align="end"
          sideOffset={4}
          className="z-50 min-w-[168px] rounded-md border border-zinc-200 bg-white p-1 shadow-md"
        >
          {localeOptions.map((option) => {
            const isActive = option.value === currentLocale;

            return (
              <DropdownMenu.Item
                key={option.value}
                onSelect={() => handleSwitch(option.value)}
                className={`flex cursor-pointer items-center justify-between rounded px-3 py-2 text-sm outline-none transition-colors focus:bg-zinc-100 ${
                  isActive
                    ? "bg-zinc-100 font-medium text-zinc-900"
                    : "text-zinc-700 hover:bg-zinc-100"
                }`}
              >
                <span>{option.label}</span>
                {isActive && (
                  <span className="text-xs text-zinc-500">{option.shortLabel}</span>
                )}
              </DropdownMenu.Item>
            );
          })}
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  );
}
