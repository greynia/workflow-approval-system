export const HEADER_ACCEPT_LANGUAGE = "Accept-Language";
export const HEADER_REQUEST_ID = "X-Request-ID";
export const HEADER_MOCK_EMPLOYEE_ID = "X-Mock-Employee-Id";

export const LOCALE_COOKIE = "NEXT_LOCALE";
// 對齊後端 SecurityConstants.TOKEN_COOKIE_NAME,
// 改名時兩邊要一起改。
export const TOKEN_COOKIE = "workflow-token";

export const HTTP_STATUS = {
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  INTERNAL_SERVER_ERROR: 500,
} as const;
