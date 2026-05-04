"use client";

import type { ReactNode } from "react";
import type { UseFormRegisterReturn } from "react-hook-form";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";
import { splitDateTimeLocal } from "../_utils/date-time";

const selectClassName = cn(
  "h-8 w-full min-w-0 rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm transition-colors outline-none",
  "focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50",
  "disabled:pointer-events-none disabled:cursor-not-allowed disabled:bg-input/50 disabled:opacity-50",
  "aria-invalid:border-destructive aria-invalid:ring-3 aria-invalid:ring-destructive/20",
);

interface DateTimePartProps {
  label: string;
  allDayLabel: string;
  value: string | undefined;
  availableTimes: string[];
  dateMin?: string;
  dateMax?: string;
  registerProps: UseFormRegisterReturn;
  onDateTimeChange: (nextDate: string, nextTime: string) => void;
  onAllDayClick: () => void;
  errorSlot: ReactNode;
}

function DateTimePart({
  label,
  allDayLabel,
  value,
  availableTimes,
  dateMin,
  dateMax,
  registerProps,
  onDateTimeChange,
  onAllDayClick,
  errorSlot,
}: DateTimePartProps) {
  const parts = splitDateTimeLocal(value);

  return (
    <div className="flex flex-col gap-1.5">
      <Label>{label}</Label>
      <input type="hidden" {...registerProps} />
      <div className="grid grid-cols-[minmax(0,1fr)_120px] gap-2">
        <Input
          type="date"
          value={parts.date}
          min={dateMin || undefined}
          max={dateMax || undefined}
          onChange={(event) => onDateTimeChange(event.target.value, parts.time)}
        />
        <select
          value={parts.time}
          onChange={(event) => onDateTimeChange(parts.date, event.target.value)}
          className={selectClassName}
        >
          {availableTimes.map((time) => (
            <option key={time} value={time}>
              {time}
            </option>
          ))}
        </select>
      </div>
      <button
        type="button"
        onClick={onAllDayClick}
        className="self-start text-xs text-muted-foreground hover:text-foreground"
      >
        {allDayLabel}
      </button>
      {errorSlot}
    </div>
  );
}

interface DateTimeRangeSectionProps {
  startTime: string | undefined;
  endTime: string | undefined;
  startMaxDate?: string;
  endMinDate?: string;
  availableStartTimes: string[];
  availableEndTimes: string[];
  startRegister: UseFormRegisterReturn;
  endRegister: UseFormRegisterReturn;
  startLabel: string;
  endLabel: string;
  allDayLabel: string;
  onStartChange: (nextDate: string, nextTime: string) => void;
  onEndChange: (nextDate: string, nextTime: string) => void;
  onStartAllDay: () => void;
  onEndAllDay: () => void;
  startError: ReactNode;
  endError: ReactNode;
}

export function DateTimeRangeSection({
  startTime,
  endTime,
  startMaxDate,
  endMinDate,
  availableStartTimes,
  availableEndTimes,
  startRegister,
  endRegister,
  startLabel,
  endLabel,
  allDayLabel,
  onStartChange,
  onEndChange,
  onStartAllDay,
  onEndAllDay,
  startError,
  endError,
}: DateTimeRangeSectionProps) {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
      <DateTimePart
        label={startLabel}
        allDayLabel={allDayLabel}
        value={startTime}
        availableTimes={availableStartTimes}
        dateMax={startMaxDate}
        registerProps={startRegister}
        onDateTimeChange={onStartChange}
        onAllDayClick={onStartAllDay}
        errorSlot={startError}
      />
      <DateTimePart
        label={endLabel}
        allDayLabel={allDayLabel}
        value={endTime}
        availableTimes={availableEndTimes}
        dateMin={endMinDate}
        registerProps={endRegister}
        onDateTimeChange={onEndChange}
        onAllDayClick={onEndAllDay}
        errorSlot={endError}
      />
    </div>
  );
}
