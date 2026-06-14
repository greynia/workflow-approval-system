import { TOKEN_COOKIE } from "@/constants/app.constant";

const MOCK_SESSION_PREFIX = "mock-";

export function isMockApiEnabled(): boolean {
  return process.env.NEXT_PUBLIC_ENABLE_MSW === "true";
}

export function createMockSessionToken(employeeId: number): string {
  return `${MOCK_SESSION_PREFIX}${employeeId}`;
}

export function parseMockEmployeeId(token: string | null | undefined): number | null {
  if (!token?.startsWith(MOCK_SESSION_PREFIX)) return null;

  const rawId = token.slice(MOCK_SESSION_PREFIX.length);
  const employeeId = Number(rawId);

  return Number.isInteger(employeeId) && employeeId > 0 ? employeeId : null;
}

export function parseCookieValue(
  cookieHeader: string | null | undefined,
  cookieName: string
): string | null {
  if (!cookieHeader) return null;

  const cookies = cookieHeader.split(";").map((part) => part.trim());
  const matched = cookies.find((part) => part.startsWith(`${cookieName}=`));

  if (!matched) return null;

  return decodeURIComponent(matched.slice(cookieName.length + 1));
}

export function readMockEmployeeIdFromCookieHeader(
  cookieHeader: string | null | undefined
): number | null {
  const token = parseCookieValue(cookieHeader, TOKEN_COOKIE);
  return parseMockEmployeeId(token);
}

export function persistMockSession(employeeId: number): void {
  if (!isMockApiEnabled() || typeof document === "undefined") return;

  document.cookie = `${TOKEN_COOKIE}=${createMockSessionToken(employeeId)}; Path=/; SameSite=Lax`;
}

export function clearMockSession(): void {
  if (!isMockApiEnabled() || typeof document === "undefined") return;

  document.cookie = `${TOKEN_COOKIE}=; Path=/; Max-Age=0; SameSite=Lax`;
}

export function readMockEmployeeIdFromDocumentCookie(): number | null {
  if (typeof document === "undefined") return null;

  const token = parseCookieValue(document.cookie, TOKEN_COOKIE);
  return parseMockEmployeeId(token);
}
