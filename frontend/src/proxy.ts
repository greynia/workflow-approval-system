import createMiddleware from "next-intl/middleware";
import { NextResponse, type NextRequest } from "next/server";
import { routing } from "./i18n/routing";
import { appConfig } from "./configs/app.config";
import { TOKEN_COOKIE } from "./constants/app.constant";

const intlMiddleware = createMiddleware(routing);

// 公開路徑:未登入使用者可以訪問,已登入使用者會被 redirect 回首頁。
// localePrefix 設為 "never",所以路徑不含 locale 前綴。
const PUBLIC_PATHS: readonly string[] = [appConfig.routes.login];

function isPublicPath(pathname: string): boolean {
  return PUBLIC_PATHS.some(
    (p) => pathname === p || pathname.startsWith(`${p}/`)
  );
}

export default function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const hasToken = request.cookies.has(TOKEN_COOKIE);
  const publicPath = isPublicPath(pathname);

  // 已登入還訪問 /login:直接送回首頁,避免重複登入。
  if (hasToken && publicPath) {
    return NextResponse.redirect(new URL(appConfig.routes.home, request.url));
  }

  // 未登入訪問受保護頁面:擋下來送回 /login。
  // 這裡是 coarse check — 只確認 cookie 存在,不驗證內容。
  // Cookie 內容失效時,由 base-service.ts 的 axios 攔截器處理(收到 401 就 redirect)。
  if (!hasToken && !publicPath) {
    return NextResponse.redirect(new URL(appConfig.routes.login, request.url));
  }

  // 通過 auth 檢查後,交給 next-intl 處理 locale。
  return intlMiddleware(request);
}

export const config = {
  matcher: ["/((?!api|_next|_vercel|.*\\..*).*)"],
};
