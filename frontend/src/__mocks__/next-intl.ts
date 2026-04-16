// Manual mock for next-intl — resolves real English strings from en.json
// so test assertions can use human-readable text instead of i18n keys.
import type { ReactNode } from "react";
import enMessages from "@/i18n/messages/en.json";

type Messages = Record<string, unknown>;

function resolve(obj: Messages, keys: string[]): string | undefined {
  let current: unknown = obj;
  for (const key of keys) {
    if (typeof current !== "object" || current === null) return undefined;
    current = (current as Messages)[key];
  }
  return typeof current === "string" ? current : undefined;
}

function makeTranslator(namespace: string) {
  return function t(key: string, params?: Record<string, string | number>) {
    const fullPath = namespace ? `${namespace}.${key}` : key;
    const parts = fullPath.split(".");
    const value = resolve(enMessages as Messages, parts) ?? key;
    if (!params) return value;
    return value.replace(/\{(\w+)\}/g, (_, k) => String(params[k] ?? `{${k}}`));
  };
}

export const useTranslations = (namespace = "") => makeTranslator(namespace);

export const NextIntlClientProvider = ({
  children,
}: {
  children: ReactNode;
  locale?: string;
  messages?: unknown;
}) => children as React.ReactElement;

export const useLocale = () => "en";
export const useMessages = () => enMessages;

// next-intl/routing subpath
export const defineRouting = (config: Record<string, unknown>) => config;

// next-intl/navigation subpath
export const createNavigation = () => ({
  Link: ({ href, children, ...rest }: { href: string; children: ReactNode; [k: string]: unknown }) =>
    ({ type: "a", props: { href, ...rest, children } }) as unknown as React.ReactElement,
  redirect: (href: string) => href,
  usePathname: () => "/",
  useRouter: () => ({ replace: () => {}, push: () => {} }),
  getPathname: () => "/",
});
