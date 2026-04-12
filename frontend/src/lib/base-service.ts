import axios, { AxiosError, type AxiosInstance } from "axios";
import { appConfig } from "@/configs/app.config";
import {
  HEADER_ACCEPT_LANGUAGE,
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
  return config;
});

BaseService.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorResponse>) => {
    const status = error.response?.status;
    const isNetworkError = !error.response;
    const isLoginPage =
      typeof window !== "undefined" &&
      window.location.pathname.endsWith(appConfig.routes.login);

    if (status === HTTP_STATUS.UNAUTHORIZED && !isLoginPage) {
      useAuthStore.getState().clearUser();
      window.location.assign(appConfig.routes.login);
    } else if (isNetworkError) {
      useToastStore.getState().error("網路連線失敗");
    } else if (status && status >= HTTP_STATUS.INTERNAL_SERVER_ERROR) {
      useToastStore.getState().error("伺服器錯誤");
    }

    return Promise.reject(error);
  }
);

export default BaseService;
