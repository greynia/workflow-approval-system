"use client";

import { useState } from "react";
import { Input } from "@/components/ui/input";
import type { EmployeeSummary } from "@/types/common";

export interface DeputySearchLabels {
  placeholderEnabled: string;
  placeholderDisabled: string;
  loading: string;
  noResults: string;
}

interface DeputySearchProps {
  selectedDeputyId: number | undefined;
  employees: EmployeeSummary[];
  isLoading: boolean;
  enabled: boolean;
  query: string;
  onQueryChange: (next: string) => void;
  onSelect: (id: number | undefined) => void;
  labels: DeputySearchLabels;
}

export function DeputySearch({
  selectedDeputyId,
  employees,
  isLoading,
  enabled,
  query,
  onQueryChange,
  onSelect,
  labels,
}: DeputySearchProps) {
  const [showDropdown, setShowDropdown] = useState(false);
  const selectedEmployee =
    selectedDeputyId != null ? employees.find((e) => e.id === selectedDeputyId) : undefined;

  return (
    <div className="relative">
      <Input
        type="text"
        value={selectedEmployee ? selectedEmployee.name : query}
        disabled={!enabled}
        placeholder={enabled ? labels.placeholderEnabled : labels.placeholderDisabled}
        onChange={(event) => {
          onQueryChange(event.target.value);
          onSelect(undefined);
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
      />
      {showDropdown && !selectedEmployee && enabled && (
        <div className="absolute z-10 mt-1 max-h-48 w-full overflow-auto rounded-md border border-border bg-popover text-popover-foreground shadow-md">
          {isLoading ? (
            <p className="px-3 py-2 text-sm text-muted-foreground">{labels.loading}</p>
          ) : employees.length === 0 ? (
            <p className="px-3 py-2 text-sm text-muted-foreground">{labels.noResults}</p>
          ) : (
            <ul>
              {employees.map((employee) => (
                <li key={employee.id}>
                  <button
                    type="button"
                    onMouseDown={(e) => {
                      e.preventDefault();
                      onSelect(employee.id);
                      onQueryChange("");
                      setShowDropdown(false);
                    }}
                    className="flex w-full cursor-pointer items-center justify-between px-3 py-2 text-left text-sm text-foreground hover:bg-muted"
                  >
                    <span>{employee.name}</span>
                    <span className="text-xs text-muted-foreground">{employee.employeeNo}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
