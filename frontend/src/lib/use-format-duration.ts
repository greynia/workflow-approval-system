import { useCallback, useMemo } from "react";
import { useTranslations } from "next-intl";
import { formatDuration, formatDurationAsHours } from "./format-duration";

export function useFormatDuration(): (minutes: number) => string {
  const t = useTranslations("Common.Duration");
  const units = useMemo(
    () => ({ day: t("day"), hour: t("hour"), min: t("min") }),
    [t]
  );
  return useCallback((minutes: number) => formatDuration(minutes, units), [units]);
}

export function useFormatDurationAsHours(): (minutes: number) => string {
  const t = useTranslations("Common.Duration");
  const units = useMemo(
    () => ({ day: t("day"), hour: t("hour"), min: t("min") }),
    [t]
  );
  return useCallback((minutes: number) => formatDurationAsHours(minutes, units), [units]);
}
