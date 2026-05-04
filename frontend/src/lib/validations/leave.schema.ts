import { z } from "zod";

const LEAVE_TYPES = ["ANNUAL", "SICK", "PERSONAL", "OTHER"] as const;

function isHalfHourAligned(val: string): boolean {
  if (!val) return true;
  const date = new Date(val);
  if (isNaN(date.getTime())) return true;
  return date.getMinutes() === 0 || date.getMinutes() === 30;
}

const baseSchema = z.object({
  type: z.enum(LEAVE_TYPES, { error: "TypeRequired" }),
  startTime: z
    .string()
    .min(1, { message: "StartDateRequired" })
    .refine(isHalfHourAligned, { message: "InvalidTimeUnit" }),
  endTime: z
    .string()
    .min(1, { message: "EndDateRequired" })
    .refine(isHalfHourAligned, { message: "InvalidTimeUnit" }),
  reason: z.string().max(1000).optional(),
  deputyId: z
    .number({ error: "DeputyRequired" })
    .int()
    .positive()
    .optional()
    .refine((value) => value != null, { message: "DeputyRequired" }),
});

export const createLeaveSchema = baseSchema.refine(
  (data) => !data.endTime || !data.startTime || data.endTime > data.startTime,
  { message: "EndDateBeforeStart", path: ["endTime"] }
);

export type CreateLeaveFormValues = z.infer<typeof createLeaveSchema>;
