"use client";

import { useLocale } from "next-intl";
import { useRouter, usePathname } from "@/i18n/navigation";
import { routing } from "@/i18n/routing";

const localeLabels: Record<string, string> = {
  "zh-TW": "繁中",
  en: "EN",
};

export function LocaleSwitcher() {
  const currentLocale = useLocale();
  const router = useRouter();
  const pathname = usePathname();

  function handleSwitch() {
    const nextLocale =
      currentLocale === "zh-TW" ? "en" : "zh-TW";
    router.replace(
      { pathname },
      { locale: nextLocale }
    );
  }

  return (
    <button
      type="button"
      onClick={handleSwitch}
      className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium text-zinc-700 hover:bg-zinc-100 transition-colors"
    >
      {localeLabels[currentLocale === "zh-TW" ? "en" : "zh-TW"]}
    </button>
  );
}
