"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useTranslations } from "next-intl";
import { useAuth, LoginFailedError } from "@/hooks/useAuth";
import { LocaleSwitcher } from "@/components/ui/locale-switcher";
import {
  loginSchema,
  type LoginFormValues,
} from "@/lib/validations/auth.schema";

export default function LoginPage() {
  const t = useTranslations("Auth.Login");
  const tLoginErrors = useTranslations("Auth.Login.Errors");
  const tError = useTranslations("Error");
  const { signIn } = useAuth();

  const {
    register,
    handleSubmit,
    setError,
    clearErrors,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    mode: "onSubmit",
    reValidateMode: "onChange",
    defaultValues: { email: "", password: "" },
  });

  const clearRootError = () => {
    if (errors.root) clearErrors("root");
  };

  const onSubmit = async (values: LoginFormValues) => {
    try {
      await signIn(values);
    } catch (err) {
      if (err instanceof LoginFailedError) {
        setError("root", {
          type: "server",
          message: tError(err.code) || tError("Default"),
        });
      }
    }
  };

  const rootError = errors.root?.message;

  return (
    <div className="relative flex min-h-full items-center justify-center px-4">
      <div className="absolute top-4 right-4">
        <LocaleSwitcher />
      </div>

      <div className="w-full max-w-sm space-y-6">
        <div className="text-center">
          <h1 className="text-2xl font-bold tracking-tight">{t("Title")}</h1>
          <p className="mt-2 text-sm text-zinc-500">{t("Subtitle")}</p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          {rootError && (
            <div
              role="alert"
              className="rounded-md bg-red-50 p-3 text-sm text-red-700"
            >
              {rootError}
            </div>
          )}

          <div>
            <label
              htmlFor="email"
              className="block text-sm font-medium text-zinc-700"
            >
              {t("Email")}
            </label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              aria-invalid={errors.email ? "true" : "false"}
              {...register("email", { onChange: clearRootError })}
              className="mt-1 block w-full rounded-md border border-zinc-300 px-3 py-2 text-sm shadow-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500 aria-[invalid=true]:border-red-400"
              placeholder={t("EmailPlaceholder")}
            />
            {errors.email?.message && (
              <p className="mt-1 text-xs text-red-600">
                {tLoginErrors(
                  errors.email.message as Parameters<typeof tLoginErrors>[0]
                )}
              </p>
            )}
          </div>

          <div>
            <label
              htmlFor="password"
              className="block text-sm font-medium text-zinc-700"
            >
              {t("Password")}
            </label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              aria-invalid={errors.password ? "true" : "false"}
              {...register("password", { onChange: clearRootError })}
              className="mt-1 block w-full rounded-md border border-zinc-300 px-3 py-2 text-sm shadow-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500 aria-[invalid=true]:border-red-400"
            />
            {errors.password?.message && (
              <p className="mt-1 text-xs text-red-600">
                {tLoginErrors(
                  errors.password.message as Parameters<typeof tLoginErrors>[0]
                )}
              </p>
            )}
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-800 disabled:opacity-50"
          >
            {isSubmitting ? t("Loading") : t("Submit")}
          </button>
        </form>
      </div>
    </div>
  );
}
