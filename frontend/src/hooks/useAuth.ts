import { isAxiosError } from "axios";
import { useTranslations } from "next-intl";
import { useQueryClient } from "@tanstack/react-query";
import { useRouter } from "@/i18n/navigation";
import AuthService from "@/services/auth.service";
import { useAuthStore } from "@/stores/auth-store";
import { useToastStore } from "@/stores/toast-store";
import { HTTP_STATUS } from "@/constants/app.constant";
import { appConfig } from "@/configs/app.config";
import type { LoginRequest } from "@/types/auth";
import { clearMockSession, persistMockSession } from "@/lib/mock-api";

export class LoginFailedError extends Error {
  constructor(public readonly code: string) {
    super(code);
    this.name = "LoginFailedError";
  }
}

export function useAuth() {
  const router = useRouter();
  const tNotification = useTranslations("Common.Notification");
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);
  const setUser = useAuthStore((s) => s.setUser);
  const clearUser = useAuthStore((s) => s.clearUser);

  const signIn = async (credentials: LoginRequest) => {
    try {
      const data = await AuthService.signIn(credentials);
      persistMockSession(data.employeeId);
      setUser(data);
      useToastStore.getState().success(tNotification("LoginSuccess"));
      router.push("/");
    } catch (err) {
      if (
        isAxiosError(err) &&
        (err.response?.status === HTTP_STATUS.UNAUTHORIZED ||
          err.response?.status === HTTP_STATUS.BAD_REQUEST)
      ) {
        throw new LoginFailedError(err.response.data?.code ?? "Default");
      }
      throw err;
    }
  };

  const signOut = async () => {
    try {
      await AuthService.signOut();
    } finally {
      clearMockSession();
      queryClient.clear();
      clearUser();
      useToastStore.getState().info(tNotification("LogoutSuccess"));
      router.push(appConfig.routes.login);
    }
  };

  return {
    user,
    authenticated: !!user,
    signIn,
    signOut,
  };
}
