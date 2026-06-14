import { z } from "zod";

// Error keys are returned instead of literal strings so the UI layer can
// translate them via next-intl. Keep keys in sync with Auth.Login.Errors.*
// in src/i18n/messages/*.json.
export const loginSchema = z.object({
  email: z
      .string()
      .min(1, { message: "EmailRequired" })
      .pipe(z.email({ message: "EmailInvalid" })),
    password: z
      .string()
      .min(1, { message: "PasswordRequired" })
      .min(8, { message: "PasswordTooShort" }),
  });

export type LoginFormValues = z.infer<typeof loginSchema>;
