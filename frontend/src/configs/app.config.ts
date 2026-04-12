// Locale 相關的設定不放在這裡 — i18n/routing.ts 是 next-intl 的權威來源,
// 需要 defaultLocale / supportedLocales 時,呼叫端直接 import routing。
export const appConfig = {
  apiBaseUrl: "/api",
  routes: {
    login: "/login",
    home: "/",
    requests: "/requests",
    requestsNew: "/requests/new",
    approvals: "/approvals",
    auditLogs: "/admin/audit-logs",
  },
} as const;
