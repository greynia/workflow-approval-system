"use client";

import { useEffect, useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { useDebounce } from "@/lib/use-debounce";
import { zodResolver } from "@hookform/resolvers/zod";
import { isAxiosError } from "axios";
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
import type { ApiErrorResponse } from "@/types/common";
import { useFormatDurationAsHours } from "@/lib/use-format-duration";

const LEAVE_TYPES = ["ANNUAL", "SICK", "PERSONAL", "OTHER"] as const;
const TIME_OPTIONS = Array.from({ length: 48 }, (_, index) => {
  const hours = String(Math.floor(index / 2)).padStart(2, "0");
  const minutes = index % 2 === 0 ? "00" : "30";
  return `${hours}:${minutes}`;
});

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

function toAllDay(value: string, endOfDay: boolean): string {
  if (!value) return value;
  const datePart = value.slice(0, 10);
  return `${datePart}T${endOfDay ? "18:00" : "09:00"}`;
}

function splitDateTimeLocal(value?: string): { date: string; time: string } {
  if (!value || !value.includes("T")) {
    return { date: "", time: "09:00" };
  }

  const [date, rawTime] = value.split("T");
  return { date, time: rawTime.slice(0, 5) || "09:00" };
}

function buildDateTimeLocal(date: string, time: string): string {
  if (!date || !time) return "";
  return `${date}T${time}`;
}

function formatDatePart(date: Date): string {
  const year = String(date.getFullYear());
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function formatTimePart(date: Date): string {
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");
  return `${hours}:${minutes}`;
}

function addMinutes(value: string, minutes: number): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  date.setMinutes(date.getMinutes() + minutes);
  return buildDateTimeLocal(formatDatePart(date), formatTimePart(date));
}

function getMinimumEndTime(startTime?: string): string {
  if (!startTime) return "";
  return addMinutes(startTime, 30);
}

function getMaximumStartTime(endTime?: string): string {
  if (!endTime) return "";
  return addMinutes(endTime, -30);
}

function normalizeStartTime(nextStartDate: string, nextStartTime: string, endTime?: string): string {
  const candidate = buildDateTimeLocal(nextStartDate, nextStartTime);
  if (!candidate) return "";
  if (!endTime || candidate < endTime) return candidate;
  return getMaximumStartTime(endTime);
}

function normalizeEndTime(nextEndDate: string, nextEndTime: string, startTime?: string): string {
  const candidate = buildDateTimeLocal(nextEndDate, nextEndTime);
  if (!candidate) return "";
  if (!startTime || candidate > startTime) return candidate;
  return getMinimumEndTime(startTime);
}

function isHalfHourAligned(value?: string): boolean {
  if (!value) return false;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return false;
  return date.getMinutes() === 0 || date.getMinutes() === 30;
}

export default function RequestsNewPage() {
  const t = useTranslations("Requests.New");
  const tError = useTranslations("Error");
  const formatDurationAsHours = useFormatDurationAsHours();
  const router = useRouter();
  const queryClient = useQueryClient();
  const toast = useToastStore();

  const {
    register,
    handleSubmit,
    setValue,
    control,
    formState: { errors, isSubmitting },
  } = useForm<CreateLeaveFormValues>({
    resolver: zodResolver(createLeaveSchema),
    defaultValues: { durationMinutes: 0 } as Partial<CreateLeaveFormValues>,
  });

  const startTimeField = register("startTime");
  const endTimeField = register("endTime");

  const startTime = useWatch({ control, name: "startTime" });
  const endTime = useWatch({ control, name: "endTime" });
  const selectedType = useWatch({ control, name: "type" });
  const selectedDeputyId = useWatch({ control, name: "deputyId" });
  const durationMinutes = useWatch({ control, name: "durationMinutes" });
  const [deputyQuery, setDeputyQuery] = useState("");
  const [showDropdown, setShowDropdown] = useState(false);
  const startParts = splitDateTimeLocal(startTime);
  const endParts = splitDateTimeLocal(endTime);
  const maximumStartTime = getMaximumStartTime(endTime);
  const maximumStartParts = splitDateTimeLocal(maximumStartTime);
  const minimumEndTime = getMinimumEndTime(startTime);
  const minimumEndParts = splitDateTimeLocal(minimumEndTime);
  const availableStartTimes =
    endParts.date && startParts.date === endParts.date
      ? TIME_OPTIONS.filter((time) => time < endParts.time)
      : TIME_OPTIONS;
  const availableEndTimes =
    startParts.date && endParts.date === startParts.date
      ? TIME_OPTIONS.filter((time) => time > startParts.time)
      : TIME_OPTIONS;

  const debouncedStartTime = useDebounce(startTime, 500);
  const debouncedEndTime = useDebounce(endTime, 500);
  const debouncedDeputyQuery = useDebounce(deputyQuery, 250);
  const hasAlignedTimes = isHalfHourAligned(debouncedStartTime) && isHalfHourAligned(debouncedEndTime);
  const hasCompleteWindow = Boolean(
    debouncedStartTime && debouncedEndTime && debouncedEndTime > debouncedStartTime
  );
  const canCalculate = hasCompleteWindow && hasAlignedTimes;

  const { data: calculation } = useQuery({
    queryKey: ["requests", "calculate", debouncedStartTime, debouncedEndTime],
    queryFn: () => LeaveService.calculate(debouncedStartTime, debouncedEndTime),
    enabled: canCalculate,
  });

  useEffect(() => {
    if (calculation && canCalculate) {
      setValue("durationMinutes", calculation.durationMinutes, { shouldValidate: true });
      return;
    }

    setValue("durationMinutes", 0, { shouldValidate: false });
  }, [calculation, canCalculate, setValue]);

  const { data: employees = [], isLoading: isDeputiesLoading } = useQuery({
    queryKey: ["employees", "available-deputies", debouncedStartTime, debouncedEndTime, debouncedDeputyQuery],
    queryFn: () => EmployeeService.getAvailableDeputies(debouncedStartTime, debouncedEndTime, debouncedDeputyQuery),
    enabled: canCalculate,
  });

  const { data: balances } = useQuery({
    queryKey: ["requests", "balance"],
    queryFn: () => LeaveService.getBalances(),
    enabled: !!selectedType && selectedType !== "OTHER",
  });

  const currentBalance = balances?.find((item) => item.leaveType === selectedType);

  useEffect(() => {
    if (!startTime || !endTime) {
      return;
    }
    if (endTime > startTime) {
      return;
    }

    setValue("endTime", getMinimumEndTime(startTime), {
      shouldDirty: true,
      shouldValidate: true,
    });
  }, [endTime, setValue, startTime]);

  useEffect(() => {
    if (!canCalculate) {
      setValue("deputyId", undefined as unknown as number, { shouldDirty: true });
      return;
    }
    if (selectedDeputyId != null && !employees.some((employee) => employee.id === selectedDeputyId)) {
      setValue("deputyId", undefined as unknown as number, { shouldDirty: true });
    }
  }, [employees, canCalculate, selectedDeputyId, setValue]);

  const KNOWN_ERROR_CODES = new Set([
    "DEPUTY_ON_LEAVE",
    "APPLICANT_ON_LEAVE",
    "INSUFFICIENT_LEAVE_BALANCE",
    "INVALID_DURATION_UNIT",
    "UNSUPPORTED_EMPLOYEE_SCHEDULE",
    "BAD_REQUEST",
    "VALIDATION_ERROR",
    "RESOURCE_NOT_FOUND",
    "UNAUTHORIZED",
    "INTERNAL_SERVER_ERROR",
  ]);

  const { mutate, isPending } = useMutation({
    mutationFn: LeaveService.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["requests"] });
      toast.success(t("Success"));
      router.push(appConfig.routes.requests);
    },
    onError: (error) => {
      if (isAxiosError<ApiErrorResponse>(error)) {
        const raw = error.response?.data?.message ?? error.response?.data?.code ?? "";
        const key = KNOWN_ERROR_CODES.has(raw) ? raw : "Default";
        toast.error(tError(key as Parameters<typeof tError>[0]));
      }
    },
  });

  function onSubmit(values: CreateLeaveFormValues) {
    mutate({
      type: values.type,
      startTime: values.startTime,
      endTime: values.endTime,
      reason: values.reason,
      deputyId: values.deputyId,
    });
  }

  const busy = isSubmitting || isPending;
  const canSubmit = canCalculate && (calculation?.durationMinutes ?? durationMinutes ?? 0) >= 30;
  const allDayEndTime = toAllDay(endTime ?? startTime ?? "", true);
  const allDayEndParts = splitDateTimeLocal(allDayEndTime);

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="mb-6 text-xl font-semibold text-zinc-900">{t("Title")}</h1>

      <div className="rounded-lg border border-zinc-200 bg-white p-6">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          <div>
            <label className="mb-1.5 block text-sm font-medium text-zinc-700">
              {t("Fields.Type")}
            </label>
            <select {...register("type")} className={fieldClassName}>
              <option value="">{t("TypeOptions.Placeholder")}</option>
              {LEAVE_TYPES.map((type) => (
                <option key={type} value={type}>
                  {t(`TypeOptions.${type}`)}
                </option>
              ))}
            </select>
            <FieldError message={errors.type?.message} t={t} />
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="mb-1.5 block text-sm font-medium text-zinc-700">
                {t("Fields.StartDate")}
              </label>
              <input type="hidden" {...startTimeField} />
              <div className="grid grid-cols-[minmax(0,1fr)_120px] gap-2">
                <input
                  type="date"
                  value={startParts.date}
                  max={maximumStartParts.date || undefined}
                  onChange={(event) =>
                    setValue(
                      "startTime",
                      normalizeStartTime(event.target.value, startParts.time, endTime),
                      {
                        shouldDirty: true,
                        shouldValidate: true,
                      }
                    )
                  }
                  className={fieldClassName}
                />
                <select
                  value={startParts.time}
                  onChange={(event) =>
                    setValue(
                      "startTime",
                      normalizeStartTime(startParts.date, event.target.value, endTime),
                      {
                        shouldDirty: true,
                        shouldValidate: true,
                      }
                    )
                  }
                  className={fieldClassName}
                >
                  {availableStartTimes.map((time) => (
                    <option key={time} value={time}>
                      {time}
                    </option>
                  ))}
                </select>
              </div>
              <button
                type="button"
                onClick={() =>
                  setValue(
                    "startTime",
                    normalizeStartTime(
                      splitDateTimeLocal(toAllDay(startTime ?? "", false)).date,
                      splitDateTimeLocal(toAllDay(startTime ?? "", false)).time,
                      endTime
                    ),
                    { shouldDirty: true, shouldValidate: true }
                  )
                }
                className="mt-2 text-xs text-zinc-500 hover:text-zinc-700"
              >
                {t("AllDay")}
              </button>
              <FieldError message={errors.startTime?.message} t={t} />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-zinc-700">
                {t("Fields.EndDate")}
              </label>
              <input type="hidden" {...endTimeField} />
              <div className="grid grid-cols-[minmax(0,1fr)_120px] gap-2">
                <input
                  type="date"
                  value={endParts.date}
                  min={minimumEndParts.date || startParts.date || undefined}
                  onChange={(event) =>
                    setValue(
                      "endTime",
                      normalizeEndTime(event.target.value, endParts.time, startTime),
                      {
                        shouldDirty: true,
                        shouldValidate: true,
                      }
                    )
                  }
                  className={fieldClassName}
                />
                <select
                  value={endParts.time}
                  onChange={(event) =>
                    setValue(
                      "endTime",
                      normalizeEndTime(endParts.date, event.target.value, startTime),
                      {
                        shouldDirty: true,
                        shouldValidate: true,
                      }
                    )
                  }
                  className={fieldClassName}
                >
                  {availableEndTimes.map((time) => (
                    <option key={time} value={time}>
                      {time}
                    </option>
                  ))}
                </select>
              </div>
              <button
                type="button"
                onClick={() =>
                  setValue(
                    "endTime",
                    normalizeEndTime(allDayEndParts.date, allDayEndParts.time, startTime),
                    { shouldDirty: true, shouldValidate: true }
                  )
                }
                className="mt-2 text-xs text-zinc-500 hover:text-zinc-700"
              >
                {t("AllDay")}
              </button>
              <FieldError message={errors.endTime?.message} t={t} />
            </div>
          </div>

          <div>
            <label className="mb-1.5 block text-sm font-medium text-zinc-700">
              {t("Fields.Days")}
            </label>
            <input type="hidden" {...register("durationMinutes", { valueAsNumber: true })} />
            <div className="rounded-md border border-zinc-200 bg-zinc-50 px-3 py-2 text-sm text-zinc-700">
              {canCalculate && (calculation?.durationMinutes ?? durationMinutes ?? 0) > 0
                ? formatDurationAsHours(calculation?.durationMinutes ?? durationMinutes ?? 0)
                : "-"}
            </div>
            <FieldError message={errors.durationMinutes?.message} t={t} />
            {currentBalance ? (
              <p className="mt-2 text-xs text-zinc-500">
                {t("BalanceHint", {
                  leaveType: t(`TypeOptions.${selectedType as "ANNUAL" | "SICK" | "PERSONAL" | "OTHER"}`),
                  remaining: formatDurationAsHours(currentBalance.remainingMinutes),
                  used: formatDurationAsHours(currentBalance.usedMinutes),
                  quota: formatDurationAsHours(currentBalance.quotaMinutes),
                })}
              </p>
            ) : null}
          </div>

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

          <div>
            <label className="mb-1.5 block text-sm font-medium text-zinc-700">
              {t("Fields.DeputyId")}
            </label>
            <input
              type="hidden"
              {...register("deputyId", {
                setValueAs: (value) => {
                  if (value === "" || value == null) {
                    return undefined;
                  }
                  return Number(value);
                },
              })}
            />
            {(() => {
              const selectedEmployee = selectedDeputyId != null
                ? employees.find((e) => e.id === selectedDeputyId)
                : undefined;
              return (
                <div className="relative">
                  <input
                    type="text"
                    value={selectedEmployee ? selectedEmployee.name : deputyQuery}
                    disabled={!canCalculate}
                    placeholder={
                      !canCalculate
                        ? t("Placeholders.DeputyDateRequired")
                        : t("Placeholders.DeputySearch")
                    }
                    onChange={(event) => {
                      setDeputyQuery(event.target.value);
                      setValue("deputyId", undefined as unknown as number, { shouldDirty: true, shouldValidate: true });
                      setShowDropdown(true);
                    }}
                    onFocus={() => {
                      if (!selectedEmployee) {
                        setShowDropdown(true);
                      }
                    }}
                    onBlur={() => {
                      setTimeout(() => setShowDropdown(false), 150);
                    }}
                    className={fieldClassName}
                  />
                  {showDropdown && !selectedEmployee && canCalculate && (
                    <div className="absolute z-10 mt-1 max-h-48 w-full overflow-auto rounded-md border border-zinc-200 bg-white shadow-md">
                      {isDeputiesLoading ? (
                        <p className="px-3 py-2 text-sm text-zinc-400">
                          {t("Placeholders.DeputyLoading")}
                        </p>
                      ) : employees.length === 0 ? (
                        <p className="px-3 py-2 text-sm text-zinc-400">
                          {t("Placeholders.DeputyNoResults")}
                        </p>
                      ) : (
                        <ul>
                          {employees.map((employee) => (
                            <li key={employee.id}>
                              <button
                                type="button"
                                onMouseDown={(e) => {
                                  e.preventDefault();
                                  setValue("deputyId", employee.id, {
                                    shouldDirty: true,
                                    shouldValidate: true,
                                  });
                                  setDeputyQuery("");
                                  setShowDropdown(false);
                                }}
                                className="flex w-full cursor-pointer items-center justify-between px-3 py-2 text-left text-sm text-zinc-700 hover:bg-zinc-50"
                              >
                                <span>{employee.name}</span>
                                <span className="text-xs text-zinc-400">{employee.employeeNo}</span>
                              </button>
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                  )}
                </div>
              );
            })()}
            <FieldError message={errors.deputyId?.message} t={t} />
          </div>

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
              disabled={busy || !canSubmit}
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
