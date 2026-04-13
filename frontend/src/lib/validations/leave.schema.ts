import { z } from "zod";

const LEAVE_TYPES = ["ANNUAL", "SICK", "PERSONAL", "OTHER"] as const;

const baseSchema = z.object({
  type: z.enum(LEAVE_TYPES, { error: "TypeRequired" }),
  startDate: z.string().min(1, { message: "StartDateRequired" }),
  endDate: z.string().min(1, { message: "EndDateRequired" }),
  days: z
    .number({ error: "DaysMin" })
    .int()
    .min(1, { message: "DaysMin" }),
  reason: z.string().max(1000).optional(),
  deputyId: z.number().int().positive().optional().nullable(),
});

export const createLeaveSchema = baseSchema.refine(
  (data) => !data.endDate || !data.startDate || data.endDate >= data.startDate,
  { message: "EndDateBeforeStart", path: ["endDate"] }
);

export type CreateLeaveFormValues = z.infer<typeof createLeaveSchema>;
