export const TIME_OPTIONS = Array.from({ length: 48 }, (_, index) => {
  const hours = String(Math.floor(index / 2)).padStart(2, "0");
  const minutes = index % 2 === 0 ? "00" : "30";
  return `${hours}:${minutes}`;
});

export function toAllDay(value: string, endOfDay: boolean): string {
  if (!value) return value;
  const datePart = value.slice(0, 10);
  return `${datePart}T${endOfDay ? "18:00" : "09:00"}`;
}

export function splitDateTimeLocal(value?: string): { date: string; time: string } {
  if (!value || !value.includes("T")) {
    return { date: "", time: "09:00" };
  }

  const [date, rawTime] = value.split("T");
  return { date, time: rawTime.slice(0, 5) || "09:00" };
}

export function buildDateTimeLocal(date: string, time: string): string {
  if (!date || !time) return "";
  return `${date}T${time}`;
}

export function formatDatePart(date: Date): string {
  const year = String(date.getFullYear());
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function formatTimePart(date: Date): string {
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");
  return `${hours}:${minutes}`;
}

export function addMinutes(value: string, minutes: number): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  date.setMinutes(date.getMinutes() + minutes);
  return buildDateTimeLocal(formatDatePart(date), formatTimePart(date));
}

export function getMinimumEndTime(startTime?: string): string {
  if (!startTime) return "";
  return addMinutes(startTime, 30);
}

export function getMaximumStartTime(endTime?: string): string {
  if (!endTime) return "";
  return addMinutes(endTime, -30);
}

export function normalizeStartTime(
  nextStartDate: string,
  nextStartTime: string,
  endTime?: string,
): string {
  const candidate = buildDateTimeLocal(nextStartDate, nextStartTime);
  if (!candidate) return "";
  if (!endTime || candidate < endTime) return candidate;
  return getMaximumStartTime(endTime);
}

export function normalizeEndTime(
  nextEndDate: string,
  nextEndTime: string,
  startTime?: string,
): string {
  const candidate = buildDateTimeLocal(nextEndDate, nextEndTime);
  if (!candidate) return "";
  if (!startTime || candidate > startTime) return candidate;
  return getMinimumEndTime(startTime);
}

export function isHalfHourAligned(value?: string): boolean {
  if (!value) return false;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return false;
  return date.getMinutes() === 0 || date.getMinutes() === 30;
}
