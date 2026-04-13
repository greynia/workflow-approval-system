import axios, { isAxiosError, type AxiosInstance } from "axios";
import { appConfig } from "@/configs/app.config";
import {
  HEADER_ACCEPT_LANGUAGE,
  HEADER_MOCK_EMPLOYEE_ID,
  HEADER_REQUEST_ID,
  HTTP_STATUS,
  LOCALE_COOKIE,
} from "@/constants/app.constant";
// MEMO: X-Request-ID header 前端已送出,但後端尚未接(ApiExceptionHandler 目前自產 traceId,
// 沒從 request header 讀)。等後端加上 @RequestHeader("X-Request-ID") 並寫進 MDC /
// 回傳的 ErrorResponse.traceId 後,前後端 request tracing 才串得起來。不刪,等後端對接。
import { routing } from "@/i18n/routing";
import { useAuthStore } from "@/stores/auth-store";
import { useToastStore } from "@/stores/toast-store";
import type { ApiErrorResponse } from "@/types/common";
import { getVanillaTranslator } from "./i18n-vanilla";
import {
  clearMockSession,
  isMockApiEnabled,
  readMockEmployeeIdFromDocumentCookie,
} from "./mock-api";

function readLocaleCookie(): string {
  if (typeof document === "undefined") return routing.defaultLocale;
  const match = document.cookie.match(
    new RegExp(`(?:^|; )${LOCALE_COOKIE}=([^;]*)`)
  );
  return match ? decodeURIComponent(match[1]) : routing.defaultLocale;
}

function generateRequestId(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

const BaseService: AxiosInstance = axios.create({
  baseURL: appConfig.apiBaseUrl,
  withCredentials: true,
  timeout: 15000,
});

BaseService.interceptors.request.use((config) => {
  config.headers.set(HEADER_ACCEPT_LANGUAGE, readLocaleCookie());
  config.headers.set(HEADER_REQUEST_ID, generateRequestId());

  if (isMockApiEnabled()) {
    const employeeId = readMockEmployeeIdFromDocumentCookie();

    if (employeeId) {
      config.headers.set(HEADER_MOCK_EMPLOYEE_ID, String(employeeId));
    } else {
      config.headers.delete(HEADER_MOCK_EMPLOYEE_ID);
    }
  }

  return config;
});

BaseService.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (!isAxiosError<ApiErrorResponse>(error)) {
      return Promise.reject(error);
    }

    const status = error.response?.status;
    const isNetworkError = !error.response;
    const isLoginPage =
      typeof window !== "undefined" &&
      window.location.pathname.endsWith(appConfig.routes.login);

    if (status === HTTP_STATUS.UNAUTHORIZED && !isLoginPage) {
      clearMockSession();
      useAuthStore.getState().clearUser();
      window.location.assign(appConfig.routes.login);
    } else if (isNetworkError) {
      const t = getVanillaTranslator("Common.Notification");
      useToastStore.getState().error(t("NetworkError"));
    } else if (status && status >= HTTP_STATUS.INTERNAL_SERVER_ERROR) {
      const t = getVanillaTranslator("Common.Notification");
      useToastStore.getState().error(t("ServerError"));
    }

    return Promise.reject(error);
  }
);

export default BaseService;
