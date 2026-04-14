"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useRouter } from "@/i18n/navigation";
import {
  createLeaveSchema,
  type CreateLeaveFormValues,
} from "@/lib/validations/leave.schema";
import LeaveService from "@/services/leave.service";
import EmployeeService from "@/services/employee.service";
import { useToastStore } from "@/stores/toast-store";
import { appConfig } from "@/configs/app.config";

const LEAVE_TYPES = ["ANNUAL", "SICK", "PERSONAL", "OTHER"] as const;


const fieldClassName =
  "w-full rounded-md border border-zinc-300 bg-white px-3 py-2 text-sm text-zinc-900 placeholder:text-zinc-400 focus:outline-none focus:ring-2 focus:ring-zinc-900";

type NewTranslations = ReturnType<typeof useTranslations<"Requests.New">>;

function FieldError({ message, t }: { message?: string; t: NewTranslations }) {
  if (!message) return null;
  return (
    <p className="mt-1 text-xs text-red-600">
      {t(`Errors.${message}` as Parameters<NewTranslations>[0])}
    </p>
  );
}

export default function RequestsNewPage() {
  const t = useTranslations("Requests.New");
  const router = useRouter();
  const queryClient = useQueryClient();
  const toast = useToastStore();

  const {
    register,
    handleSubmit,
    setValue,
    getValues,
    formState: { errors, isSubmitting },
  } = useForm<CreateLeaveFormValues>({
    resolver: zodResolver(createLeaveSchema),
    defaultValues: { days: 1 },
  });

  function recalcDays() {
    const [start, end] = getValues(["startDate", "endDate"]);
    if (start && end && end >= start) {
      const diff =
        Math.round(
          (new Date(end).getTime() - new Date(start).getTime()) / (1000 * 60 * 60 * 24)
        ) + 1;
      setValue("days", diff, { shouldValidate: true });
    } else {
      setValue("days", 1, { shouldValidate: false });
    }
  }

  const { data: employees = [] } = useQuery({
    queryKey: ["employees"],
    queryFn: EmployeeService.getList,
  });

  const { mutate, isPending } = useMutation({
    mutationFn: LeaveService.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["requests"] });
      toast.success(t("Success"));
      router.push(appConfig.routes.requests);
    },
  });

  function onSubmit(values: CreateLeaveFormValues) {
    mutate({ ...values, deputyId: values.deputyId ?? null });
  }

  const busy = isSubmitting || isPending;

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="mb-6 text-xl font-semibold text-zinc-900">{t("Title")}</h1>

      <div className="rounded-lg border border-zinc-200 bg-white p-6">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          {/* 假別 */}
          <div>
            <label className="mb-1.5 block text-sm font-medium text-zinc-700">
              {t("Fields.Type")}
            </label>
            <select
              {...register("type")}
              className={fieldClassName}
            >
              <option value="">{t("TypeOptions.Placeholder")}</option>
              {LEAVE_TYPES.map((type) => (
                <option key={type} value={type}>
                  {t(`TypeOptions.${type}`)}
                </option>
              ))}
            </select>
            <FieldError message={errors.type?.message} t={t} />
          </div>

          {/* 起迄日期 */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="mb-1.5 block text-sm font-medium text-zinc-700">
                {t("Fields.StartDate")}
              </label>
              <input
                type="date"
                {...register("startDate", { onChange: recalcDays })}
                className={fieldClassName}
              />
              <FieldError message={errors.startDate?.message} t={t} />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-zinc-700">
                {t("Fields.EndDate")}
              </label>
              <input
                type="date"
                {...register("endDate", { onChange: recalcDays })}
                className={fieldClassName}
              />
              <FieldError message={errors.endDate?.message} t={t} />
            </div>
          </div>

          {/* 天數 */}
          <div>
            <label className="mb-1.5 block text-sm font-medium text-zinc-700">
              {t("Fields.Days")}
            </label>
            <input
              type="number"
              min={1}
              {...register("days", { valueAsNumber: true })}
              className={fieldClassName}
            />
            <FieldError message={errors.days?.message} t={t} />
          </div>

          {/* 事由 */}
          <div>
            <label className="mb-1.5 block text-sm font-medium text-zinc-700">
              {t("Fields.Reason")}
            </label>
            <textarea
              {...register("reason")}
              rows={3}
              placeholder={t("Placeholders.Reason")}
              className={`${fieldClassName} resize-none`}
            />
          </div>

          {/* 代理人 */}
          <div>
            <label className="mb-1.5 block text-sm font-medium text-zinc-700">
              {t("Fields.DeputyId")}
            </label>
            <select
              {...register("deputyId", {
                setValueAs: (v) => (v === "" || v === null ? null : Number(v)),
              })}
              className={fieldClassName}
            >
              <option value="">{t("Placeholders.DeputyId")}</option>
              {employees.map((emp) => (
                <option key={emp.id} value={emp.id}>
                  {emp.name}
                </option>
              ))}
            </select>
          </div>

          {/* 操作按鈕 */}
          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={() => router.push(appConfig.routes.requests)}
              className="rounded-md border border-zinc-300 px-4 py-2 text-sm font-medium text-zinc-700 transition-colors hover:bg-zinc-50"
            >
              {t("Cancel")}
            </button>
            <button
              type="submit"
              disabled={busy}
              className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-zinc-700 disabled:opacity-50"
            >
              {busy ? t("Submitting") : t("Submit")}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
