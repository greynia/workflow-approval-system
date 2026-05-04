"use client";

import * as React from "react";

import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";

export type AuditLogFilterValues = {
  entityType?: string;
  action?: string;
  actorName?: string;
  createdFrom?: string;
  createdTo?: string;
};

export type AuditLogFilterOption = { value: string; label: string };

export type AuditLogFilterLabels = {
  entityType: string;
  action: string;
  actorName: string;
  createdFrom: string;
  createdTo: string;
  entityTypePlaceholder: string;
  actionPlaceholder: string;
  actorNamePlaceholder: string;
};

export type AuditLogFilterBarProps = {
  value: AuditLogFilterValues;
  onChange: (next: AuditLogFilterValues) => void;
  entityTypeOptions: AuditLogFilterOption[];
  actionOptions: AuditLogFilterOption[];
  labels: AuditLogFilterLabels;
};

const selectClassName = cn(
  "h-8 w-full min-w-0 rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm transition-colors outline-none",
  "focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50",
);

function AuditLogFilterBar({
  value,
  onChange,
  entityTypeOptions,
  actionOptions,
  labels,
}: AuditLogFilterBarProps) {
  function patch<K extends keyof AuditLogFilterValues>(
    key: K,
    next: AuditLogFilterValues[K],
  ) {
    onChange({ ...value, [key]: next || undefined });
  }

  return (
    <div className="flex flex-wrap gap-3 rounded-xl bg-card p-4 ring-1 ring-foreground/10">
      <div className="flex min-w-[10rem] flex-col gap-1">
        <Label htmlFor="audit-filter-entity">{labels.entityType}</Label>
        <select
          id="audit-filter-entity"
          value={value.entityType ?? ""}
          onChange={(e) => patch("entityType", e.target.value)}
          className={selectClassName}
        >
          <option value="">{labels.entityTypePlaceholder}</option>
          {entityTypeOptions.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      </div>

      <div className="flex min-w-[10rem] flex-col gap-1">
        <Label htmlFor="audit-filter-action">{labels.action}</Label>
        <select
          id="audit-filter-action"
          value={value.action ?? ""}
          onChange={(e) => patch("action", e.target.value)}
          className={selectClassName}
        >
          <option value="">{labels.actionPlaceholder}</option>
          {actionOptions.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      </div>

      <div className="flex min-w-[12rem] flex-col gap-1">
        <Label htmlFor="audit-filter-actor">{labels.actorName}</Label>
        <Input
          id="audit-filter-actor"
          type="text"
          placeholder={labels.actorNamePlaceholder}
          value={value.actorName ?? ""}
          onChange={(e) => patch("actorName", e.target.value)}
        />
      </div>

      <div className="flex min-w-[12rem] flex-col gap-1">
        <Label htmlFor="audit-filter-from">{labels.createdFrom}</Label>
        <Input
          id="audit-filter-from"
          type="datetime-local"
          value={value.createdFrom ?? ""}
          onChange={(e) => patch("createdFrom", e.target.value)}
        />
      </div>

      <div className="flex min-w-[12rem] flex-col gap-1">
        <Label htmlFor="audit-filter-to">{labels.createdTo}</Label>
        <Input
          id="audit-filter-to"
          type="datetime-local"
          value={value.createdTo ?? ""}
          onChange={(e) => patch("createdTo", e.target.value)}
        />
      </div>
    </div>
  );
}

export { AuditLogFilterBar };
