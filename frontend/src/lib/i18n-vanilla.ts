import { createTranslator, type NamespaceKeys, type NestedKeyOf } from "next-intl";
import en from "@/i18n/messages/en.json";
import zhTW from "@/i18n/messages/zh-TW.json";
import { LOCALE_COOKIE } from "@/constants/app.constant";
import { routing } from "@/i18n/routing";

type Locale = (typeof routing.locales)[number];
type AppMessages = typeof zhTW;

const messagesByLocale: Record<Locale, AppMessages> = {
  "zh-TW": zhTW,
  en,
};

function readLocale(): Locale {
  if (typeof document === "undefined") return routing.defaultLocale;
  const match = document.cookie.match(
    new RegExp(`(?:^|; )${LOCALE_COOKIE}=([^;]*)`)
  );
  const raw = match ? decodeURIComponent(match[1]) : routing.defaultLocale;
  return (routing.locales as readonly string[]).includes(raw)
    ? (raw as Locale)
    : routing.defaultLocale;
}

export function getVanillaTranslator<
  NS extends NamespaceKeys<AppMessages, NestedKeyOf<AppMessages>>
>(namespace: NS) {
  const locale = readLocale();
  return createTranslator({
    locale,
    messages: messagesByLocale[locale],
    namespace,
  });
}
