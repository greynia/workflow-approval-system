"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useTranslations } from "next-intl";
import { Workflow } from "lucide-react";
import { useAuth, LoginFailedError } from "@/hooks/useAuth";
import { Button } from "@/components/ui/button";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
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
  const emailErrorKey = errors.email?.message as
    | Parameters<typeof tLoginErrors>[0]
    | undefined;
  const passwordErrorKey = errors.password?.message as
    | Parameters<typeof tLoginErrors>[0]
    | undefined;

  return (
    <div className="grid min-h-screen grid-cols-1 md:grid-cols-2">
      {/* Left: brand area (desktop only) */}
      <aside
        aria-hidden
        className="hidden flex-col justify-between bg-gradient-to-br from-primary to-primary/80 p-12 text-primary-foreground md:flex"
      >
        <div className="flex items-center gap-2">
          <Workflow className="size-7" />
          <span className="text-lg font-semibold tracking-tight">
            {t("BrandTitle")}
          </span>
        </div>

        <div className="space-y-4">
          <h2 className="text-3xl font-semibold tracking-tight">
            {t("BrandTitle")}
          </h2>
          <p className="max-w-sm text-base text-primary-foreground/90">
            {t("BrandTagline")}
          </p>
        </div>

        <p className="text-xs text-primary-foreground/70">
          © {new Date().getFullYear()} Workflow Approval System
        </p>
      </aside>

      {/* Right: login form */}
      <main className="flex flex-col p-6 md:p-12">
        <div className="flex flex-1 items-center justify-center">
          <div className="w-full max-w-sm space-y-6">
            <div className="space-y-1">
              <h1 className="text-2xl font-semibold tracking-tight">
                {t("Heading")}
              </h1>
              <p className="text-sm text-muted-foreground">{t("Subtitle")}</p>
            </div>

            <form
              onSubmit={handleSubmit(onSubmit)}
              className="space-y-4"
              noValidate
            >
              {rootError && (
                <div
                  role="alert"
                  className="rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
                >
                  {rootError}
                </div>
              )}

              <FormField
                label={t("Email")}
                error={emailErrorKey ? tLoginErrors(emailErrorKey) : undefined}
                register={register("email", { onChange: clearRootError })}
              >
                {(props) => (
                  <Input
                    {...props}
                    type="email"
                    autoComplete="email"
                    placeholder={t("EmailPlaceholder")}
                    className="h-10"
                  />
                )}
              </FormField>

              <FormField
                label={t("Password")}
                error={
                  passwordErrorKey ? tLoginErrors(passwordErrorKey) : undefined
                }
                register={register("password", { onChange: clearRootError })}
              >
                {(props) => (
                  <Input
                    {...props}
                    type="password"
                    autoComplete="current-password"
                    className="h-10"
                  />
                )}
              </FormField>

              <Button type="submit" disabled={isSubmitting} className="w-full">
                {isSubmitting ? t("Loading") : t("Submit")}
              </Button>
            </form>
          </div>
        </div>

        <div className="flex justify-end pt-6">
          <LocaleSwitcher />
        </div>
      </main>
    </div>
  );
}
