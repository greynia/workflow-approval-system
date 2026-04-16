export type DurationUnits = { day: string; hour: string; min: string };

const DEFAULT_UNITS: DurationUnits = { day: "d", hour: "h", min: "m" };

export function formatDuration(
  durationMinutes: number,
  units: DurationUnits = DEFAULT_UNITS
): string {
  const fullDays = Math.floor(durationMinutes / 480);
  const remainingMinutes = durationMinutes % 480;

  if (remainingMinutes === 0) return `${fullDays}${units.day}`;

  const hours = Math.floor(remainingMinutes / 60);
  const mins = remainingMinutes % 60;
  const timePart =
    hours > 0 && mins > 0
      ? `${hours}${units.hour} ${mins}${units.min}`
      : hours > 0
        ? `${hours}${units.hour}`
        : `${mins}${units.min}`;

  if (fullDays === 0) return timePart;
  return `${fullDays}${units.day} ${timePart}`;
}

/**
 * Format duration as total hours + minutes (no day conversion).
 * Used for the form's calculated duration field where showing hours is clearer.
 * e.g. 1920 min → "32h", 510 min → "8h 30m", 30 min → "30m"
 */
export function formatDurationAsHours(
  durationMinutes: number,
  units: DurationUnits = DEFAULT_UNITS
): string {
  const hours = Math.floor(durationMinutes / 60);
  const mins = durationMinutes % 60;
  if (hours > 0 && mins > 0) return `${hours}${units.hour} ${mins}${units.min}`;
  if (hours > 0) return `${hours}${units.hour}`;
  return `${mins}${units.min}`;
}
