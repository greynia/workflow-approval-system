import { z } from "zod";

// Error keys are returned instead of literal strings so the UI layer can
// translate them via next-intl. Keep keys in sync with Auth.Login.Errors.*
// in src/i18n/messages/*.json.
export const loginSchema = z.object({
  // TODO(human): define validation rules for email and password.
  //
  // Requirements:
  //   - email: must be a non-empty, well-formed email address.
  //     On empty → return { message: "EmailRequired" }
  //     On invalid format → return { message: "EmailInvalid" }
  //   - password: must be non-empty and at least 8 characters.
  //     On empty → return { message: "PasswordRequired" }
  //     On too short → return { message: "PasswordTooShort" }
  //
  // Hints:
  //   - Use z.string() as the base.
  //   - For email, chain .min(1, { message: "..." }).email({ message: "..." }).
  //   - For password, chain .min(1, { message: "..." }).min(8, { message: "..." }).
  //   - The order of .min() calls matters — the first failing check wins,
  //     so put the "required" check before the "format/length" check.
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
