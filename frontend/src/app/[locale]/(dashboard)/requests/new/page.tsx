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
import { useUnsavedChangesGuard } from "@/lib/use-unsaved-changes-guard";
import { useGuardedNavigation } from "@/lib/use-guarded-navigation";
import { PageHeader } from "@/components/ui/page-header";
import { Section } from "@/components/ui/section";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { DateTimeRangeSection } from "./_components/date-time-range-section";
import { DeputySearch } from "./_components/deputy-search";
import {
  TIME_OPTIONS,
  getMaximumStartTime,
  getMinimumEndTime,
  isHalfHourAligned,
  normalizeEndTime,
  normalizeStartTime,
  splitDateTimeLocal,
  toAllDay,
} from "./_utils/date-time";

const LEAVE_TYPES = ["ANNUAL", "SICK", "PERSONAL", "OTHER"] as const;

// Native <select> styled to match Input primitive — keeps RHF integration simple
// while sharing visual tokens with the rest of the form.
const selectClassName = cn(
  "h-8 w-full min-w-0 rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm transition-colors outline-none",
  "focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50",
  "disabled:pointer-events-none disabled:cursor-not-allowed disabled:bg-input/50 disabled:opacity-50",
  "aria-invalid:border-destructive aria-invalid:ring-3 aria-invalid:ring-destructive/20",
);

type NewTranslations = ReturnType<typeof useTranslations<"Requests.New">>;

function FieldError({ message, t }: { message?: string; t: NewTranslations }) {
  if (!message) return null;
  return (
    <p className="mt-1 text-xs text-destructive" role="alert">
      {t(`Errors.${message}` as Parameters<NewTranslations>[0])}
    </p>
  );
}

export default function RequestsNewPage() {
  const t = useTranslations("Requests.New");
  const tError = useTranslations("Error");
  const formatDurationAsHours = useFormatDurationAsHours();
  const router = useRouter();
  const guardedNav = useGuardedNavigation();
  const queryClient = useQueryClient();
  const toast = useToastStore.getState();

  const {
    register,
    handleSubmit,
    setValue,
    control,
    formState: { errors, isSubmitting, isDirty, isSubmitSuccessful },
  } = useForm<CreateLeaveFormValues>({
    resolver: zodResolver(createLeaveSchema),
    defaultValues: { deputyId: undefined } as Partial<CreateLeaveFormValues>,
  });

  useUnsavedChangesGuard({ when: isDirty && !isSubmitSuccessful });

  const startTimeField = register("startTime");
  const endTimeField = register("endTime");

  const startTime = useWatch({ control, name: "startTime" });
  const endTime = useWatch({ control, name: "endTime" });
  const selectedType = useWatch({ control, name: "type" });
  const selectedDeputyId = useWatch({ control, name: "deputyId" });
  const [deputyQuery, setDeputyQuery] = useState("");
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
    if (!canCalculate || selectedDeputyId == null) return;
    if (employees.some((employee) => employee.id === selectedDeputyId)) return;

    setValue("deputyId", undefined, { shouldDirty: true });
    toast.info(t("DeputyAutoCleared"));
  }, [employees, canCalculate, selectedDeputyId, setValue, toast, t]);

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
        const apiError = error.response?.data;
        const message = apiError?.message?.trim();
        if (message && message !== apiError?.code && !KNOWN_ERROR_CODES.has(message)) {
          toast.error(message);
          return;
        }

        const key = apiError?.code && KNOWN_ERROR_CODES.has(apiError.code)
          ? apiError.code
          : "Default";
        toast.error(tError(key as Parameters<typeof tError>[0]));
      }
    },
  });

  function onSubmit(values: CreateLeaveFormValues) {
    if (values.deputyId == null) {
      return;
    }

    mutate({
      type: values.type,
      startTime: values.startTime,
      endTime: values.endTime,
      reason: values.reason,
      deputyId: values.deputyId,
    });
  }

  const busy = isSubmitting || isPending;
  const canSubmit = canCalculate && (calculation?.durationMinutes ?? 0) >= 30;
  const allDayEndTime = toAllDay(endTime ?? startTime ?? "", true);
  const allDayEndParts = splitDateTimeLocal(allDayEndTime);

  const deputyIdRegister = register("deputyId", {
    setValueAs: (value) => {
      if (value === "" || value == null) {
        return undefined;
      }
      return Number(value);
    },
  });

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-6">
      <PageHeader title={t("Title")} />

      <Section>
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="leave-type">{t("Fields.Type")}</Label>
            <select id="leave-type" {...register("type")} className={selectClassName}>
              <option value="">{t("TypeOptions.Placeholder")}</option>
              {LEAVE_TYPES.map((type) => (
                <option key={type} value={type}>
                  {t(`TypeOptions.${type}`)}
                </option>
              ))}
            </select>
            <FieldError message={errors.type?.message} t={t} />
          </div>

          <DateTimeRangeSection
            startTime={startTime}
            endTime={endTime}
            startMaxDate={maximumStartParts.date}
            endMinDate={minimumEndParts.date || startParts.date}
            availableStartTimes={availableStartTimes}
            availableEndTimes={availableEndTimes}
            startRegister={startTimeField}
            endRegister={endTimeField}
            startLabel={t("Fields.StartDate")}
            endLabel={t("Fields.EndDate")}
            allDayLabel={t("AllDay")}
            onStartChange={(nextDate, nextTime) =>
              setValue("startTime", normalizeStartTime(nextDate, nextTime, endTime), {
                shouldDirty: true,
                shouldValidate: true,
              })
            }
            onEndChange={(nextDate, nextTime) =>
              setValue("endTime", normalizeEndTime(nextDate, nextTime, startTime), {
                shouldDirty: true,
                shouldValidate: true,
              })
            }
            onStartAllDay={() => {
              const allDayStart = splitDateTimeLocal(toAllDay(startTime ?? "", false));
              setValue(
                "startTime",
                normalizeStartTime(allDayStart.date, allDayStart.time, endTime),
                { shouldDirty: true, shouldValidate: true },
              );
            }}
            onEndAllDay={() =>
              setValue(
                "endTime",
                normalizeEndTime(allDayEndParts.date, allDayEndParts.time, startTime),
                { shouldDirty: true, shouldValidate: true },
              )
            }
            startError={<FieldError message={errors.startTime?.message} t={t} />}
            endError={<FieldError message={errors.endTime?.message} t={t} />}
          />

          <div className="flex flex-col gap-1.5">
            <Label>{t("Fields.Days")}</Label>
            <div className="rounded-md border border-input bg-muted/50 px-3 py-2 text-sm text-foreground">
              {canCalculate && (calculation?.durationMinutes ?? 0) > 0
                ? formatDurationAsHours(calculation?.durationMinutes ?? 0)
                : "-"}
            </div>
            {currentBalance ? (
              <p className="text-xs text-muted-foreground">
                {t("BalanceHint", {
                  leaveType: t(`TypeOptions.${selectedType as "ANNUAL" | "SICK" | "PERSONAL" | "OTHER"}`),
                  remaining: formatDurationAsHours(currentBalance.remainingMinutes),
                  used: formatDurationAsHours(currentBalance.usedMinutes),
                  quota: formatDurationAsHours(currentBalance.quotaMinutes),
                })}
              </p>
            ) : null}
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="leave-reason">{t("Fields.Reason")}</Label>
            <Textarea
              id="leave-reason"
              {...register("reason")}
              rows={3}
              placeholder={t("Placeholders.Reason")}
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label>{t("Fields.DeputyId")}</Label>
            <input type="hidden" {...deputyIdRegister} />
            <DeputySearch
              selectedDeputyId={selectedDeputyId}
              employees={employees}
              isLoading={isDeputiesLoading}
              enabled={canCalculate}
              query={deputyQuery}
              onQueryChange={setDeputyQuery}
              onSelect={(id) =>
                setValue("deputyId", id, { shouldDirty: true, shouldValidate: true })
              }
              labels={{
                placeholderEnabled: t("Placeholders.DeputySearch"),
                placeholderDisabled: t("Placeholders.DeputyDateRequired"),
                loading: t("Placeholders.DeputyLoading"),
                noResults: t("Placeholders.DeputyNoResults"),
              }}
            />
            <FieldError message={errors.deputyId?.message} t={t} />
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => guardedNav.push(appConfig.routes.requests)}
            >
              {t("Cancel")}
            </Button>
            <Button type="submit" disabled={busy || !canSubmit}>
              {busy ? t("Submitting") : t("Submit")}
            </Button>
          </div>
        </form>
      </Section>
    </div>
  );
}
