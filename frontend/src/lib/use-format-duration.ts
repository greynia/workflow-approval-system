import { useTranslations } from "next-intl";
import { formatDuration, formatDurationAsHours } from "./format-duration";

export function useFormatDuration(): (minutes: number) => string {
  const t = useTranslations("Common.Duration");
  const units = { day: t("day"), hour: t("hour"), min: t("min") };
  return (minutes: number) => formatDuration(minutes, units);
}

export function useFormatDurationAsHours(): (minutes: number) => string {
  const t = useTranslations("Common.Duration");
  const units = { day: t("day"), hour: t("hour"), min: t("min") };
  return (minutes: number) => formatDurationAsHours(minutes, units);
}
