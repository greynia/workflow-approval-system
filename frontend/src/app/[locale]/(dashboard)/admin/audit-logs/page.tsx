"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import AuditLogService from "@/services/audit-log.service";
import type { AuditLog, AuditLogFilters } from "@/types/audit-log";

const DEFAULT_SIZE = 20;
const ENTITY_TYPE_OPTIONS: { value: string; label: string }[] = [
  { value: "LEAVE_REQUEST", label: "請假申請" },
];
const ACTION_OPTIONS = ["CREATE", "APPROVE", "REJECT", "CANCEL"] as const;

function LoadingSkeleton() {
  return (
    <div className="overflow-hidden rounded-lg border border-zinc-200 bg-white">
      <table className="w-full text-sm">
        <tbody>
          {Array.from({ length: 5 }).map((_, i) => (
            <tr key={i} className="border-b border-zinc-100 last:border-0">
              {Array.from({ length: 6 }).map((_, j) => (
                <td key={j} className="px-4 py-3">
                  <div className="h-4 animate-pulse rounded bg-zinc-100" />
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function renderDetailJson(detailJson: string): React.ReactNode {
  let parsed: Record<string, unknown>;
  try {
    parsed = JSON.parse(detailJson);
  } catch {
    return <span className="font-mono text-xs text-zinc-400">{detailJson}</span>;
  }

  return (
    <div className="space-y-2">
      {Object.entries(parsed).map(([key, value]) => {
        if (value === null || value === undefined) return null;
        const display =
          typeof value === "object" ? JSON.stringify(value) : String(value);
        return (
          <div key={key} className="flex justify-between gap-4 text-xs">
            <span className="shrink-0 text-zinc-400">{key}</span>
            <span className="text-right font-mono text-zinc-700">{display}</span>
          </div>
        );
      })}
    </div>
  );
}

function DetailDrawer({
  log,
  onClose,
}: {
  log: AuditLog;
  onClose: () => void;
}) {
  const t = useTranslations("AuditLog");

  const fields: { label: string; value: string }[] = [
    { label: t("Drawer.Fields.Time"), value: new Date(log.createdAt).toLocaleString() },
    { label: t("Drawer.Fields.Actor"), value: log.actorName },
    { label: t("Drawer.Fields.Action"), value: log.action },
    { label: t("Drawer.Fields.EntityType"), value: log.entityType },
    { label: t("Drawer.Fields.EntityId"), value: String(log.entityId) },
  ];

  return (
    <>
      <div
        className="fixed inset-0 z-40 bg-black/20"
        onClick={onClose}
      />
      <div className="fixed inset-y-0 right-0 z-50 flex w-96 flex-col bg-white shadow-xl">
        <div className="flex items-center justify-between border-b border-zinc-200 p-4">
          <h2 className="font-semibold text-zinc-900">{t("Drawer.Title")}</h2>
          <button
            onClick={onClose}
            className="rounded-md p-1 text-zinc-400 hover:bg-zinc-100 hover:text-zinc-600"
          >
            ✕
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-4">
          <div className="space-y-3">
            {fields.map(({ label, value }) => (
              <div key={label} className="flex justify-between gap-4 text-sm">
                <span className="shrink-0 text-zinc-500">{label}</span>
                <span className="text-right font-medium text-zinc-900">{value}</span>
              </div>
            ))}
          </div>

          <hr className="my-4 border-zinc-100" />

          <div>
            <p className="mb-2 text-xs font-medium uppercase tracking-wide text-zinc-400">
              {t("Drawer.Fields.Detail")}
            </p>
            {renderDetailJson(log.detailJson)}
          </div>
        </div>

        {log.entityType === "LEAVE_REQUEST" && (
          <div className="border-t border-zinc-100 p-4">
            <Link
              href={`/admin/requests/${log.entityId}`}
              className="text-sm text-blue-600 hover:underline"
            >
              {t("Drawer.ViewRequest")} →
            </Link>
          </div>
        )}
      </div>
    </>
  );
}

export default function AuditLogsPage() {
  const t = useTranslations("AuditLog");
  const [selectedLog, setSelectedLog] = useState<AuditLog | null>(null);

  const [filters, setFilters] = useState<AuditLogFilters>({
    page: 0,
    size: DEFAULT_SIZE,
  });

  function handleFilterChange(field: keyof AuditLogFilters, value: string) {
    setFilters((prev) => ({
      ...prev,
      [field]: value || undefined,
      page: 0,
    }));
  }

  function handlePageChange(direction: "prev" | "next") {
    setFilters((prev) => ({
      ...prev,
      page: direction === "prev" ? prev.page - 1 : prev.page + 1,
    }));
  }

  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin", "audit-logs", filters],
    queryFn: () => AuditLogService.getAuditLogs(filters),
  });

  return (
    <div className="mx-auto max-w-6xl">
      {selectedLog && (
        <DetailDrawer log={selectedLog} onClose={() => setSelectedLog(null)} />
      )}

      {/* Page header */}
      <div className="mb-6">
        <h1 className="text-xl font-semibold text-zinc-900">{t("Title")}</h1>
        <p className="mt-1 text-sm text-zinc-500">{t("Description")}</p>
      </div>

      {/* Filter bar */}
      <div className="mb-4 flex flex-wrap gap-3 rounded-lg border border-zinc-200 bg-white p-4">
        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-zinc-500">
            {t("Filter.EntityType")}
          </label>
          <select
            value={filters.entityType ?? ""}
            onChange={(e) => handleFilterChange("entityType", e.target.value)}
            className="h-8 rounded-md border border-zinc-300 px-2 text-sm text-zinc-700 focus:border-zinc-500 focus:outline-none"
          >
            <option value="">{t("Filter.Placeholder.EntityType")}</option>
            {ENTITY_TYPE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
        </div>

        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-zinc-500">
            {t("Filter.Action")}
          </label>
          <select
            value={filters.action ?? ""}
            onChange={(e) => handleFilterChange("action", e.target.value)}
            className="h-8 rounded-md border border-zinc-300 px-2 text-sm text-zinc-700 focus:border-zinc-500 focus:outline-none"
          >
            <option value="">{t("Filter.Placeholder.Action")}</option>
            {ACTION_OPTIONS.map((opt) => (
              <option key={opt} value={opt}>{opt}</option>
            ))}
          </select>
        </div>

        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-zinc-500">
            {t("Filter.ActorName")}
          </label>
          <input
            type="text"
            placeholder={t("Filter.Placeholder.ActorName")}
            value={filters.actorName ?? ""}
            onChange={(e) => handleFilterChange("actorName", e.target.value)}
            className="h-8 rounded-md border border-zinc-300 px-3 text-sm placeholder-zinc-400 focus:border-zinc-500 focus:outline-none"
          />
        </div>

        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-zinc-500">
            {t("Filter.CreatedFrom")}
          </label>
          <input
            type="datetime-local"
            value={filters.createdFrom ?? ""}
            onChange={(e) => handleFilterChange("createdFrom", e.target.value)}
            className="h-8 rounded-md border border-zinc-300 px-3 text-sm focus:border-zinc-500 focus:outline-none"
          />
        </div>

        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-zinc-500">
            {t("Filter.CreatedTo")}
          </label>
          <input
            type="datetime-local"
            value={filters.createdTo ?? ""}
            onChange={(e) => handleFilterChange("createdTo", e.target.value)}
            className="h-8 rounded-md border border-zinc-300 px-3 text-sm focus:border-zinc-500 focus:outline-none"
          />
        </div>
      </div>

      {/* Table / states */}
      {isLoading && <LoadingSkeleton />}
      {isError && (
        <p className="py-10 text-center text-sm text-zinc-500">{t("Error")}</p>
      )}
      {!isLoading && !isError && data && (
        <>
          {data.items.length === 0 ? (
            <p className="py-16 text-center text-sm text-zinc-400">
              {t("Empty")}
            </p>
          ) : (
            <>
            {/* Mobile card list */}
            <div className="space-y-3 md:hidden">
              {data.items.map((log) => (
                <button
                  key={log.id}
                  onClick={() => setSelectedLog(log)}
                  className="w-full rounded-lg border border-zinc-200 bg-white p-4 text-left transition-colors hover:bg-zinc-50"
                >
                  <div className="flex items-start justify-between gap-2">
                    <span className="font-medium text-zinc-900">{log.actorName}</span>
                    <span className="shrink-0 rounded-full bg-zinc-100 px-2 py-0.5 text-xs text-zinc-600">{log.action}</span>
                  </div>
                  <p className="mt-1 text-sm text-zinc-500">{log.entityType} #{log.entityId}</p>
                  <p className="mt-1 text-xs text-zinc-400">{new Date(log.createdAt).toLocaleString()}</p>
                </button>
              ))}
            </div>

            {/* Desktop table */}
            <div className="hidden overflow-x-auto rounded-lg border border-zinc-200 bg-white md:block">
              <table className="w-full text-sm">
                <thead className="border-b border-zinc-200 bg-zinc-50">
                  <tr>
                    {(
                      [
                        "Time",
                        "Actor",
                        "Action",
                        "EntityType",
                        "EntityId",
                        "Detail",
                      ] as const
                    ).map((col) => (
                      <th
                        key={col}
                        className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-zinc-500"
                      >
                        {t(`Table.${col}`)}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {data.items.map((log) => (
                    <tr
                      key={log.id}
                      onClick={() => setSelectedLog(log)}
                      className="cursor-pointer border-b border-zinc-100 last:border-0 hover:bg-zinc-50"
                    >
                      <td className="px-4 py-3 text-zinc-600">
                        {new Date(log.createdAt).toLocaleString()}
                      </td>
                      <td className="px-4 py-3 font-medium text-zinc-900">
                        {log.actorName}
                      </td>
                      <td className="px-4 py-3 text-zinc-600">{log.action}</td>
                      <td className="px-4 py-3 text-zinc-600">
                        {log.entityType}
                      </td>
                      <td className="px-4 py-3 text-zinc-600">
                        {log.entityId}
                      </td>
                      <td className="px-4 py-3">
                        <span className="text-xs text-zinc-400">
                          查看詳情 →
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            </>
          )}

          {/* Pagination */}
          {data.totalPages > 1 && (
            <div className="mt-4 flex items-center justify-between text-sm text-zinc-500">
              <span>
                {t("Pagination.PageInfo", {
                  current: data.currentPage + 1,
                  total: data.totalPages,
                  count: data.totalCount,
                })}
              </span>
              <div className="flex gap-2">
                <button
                  onClick={() => handlePageChange("prev")}
                  disabled={data.currentPage === 0}
                  className="rounded-md border border-zinc-200 px-3 py-1 text-xs hover:bg-zinc-50 disabled:opacity-40"
                >
                  {t("Pagination.Previous")}
                </button>
                <button
                  onClick={() => handlePageChange("next")}
                  disabled={data.currentPage + 1 >= data.totalPages}
                  className="rounded-md border border-zinc-200 px-3 py-1 text-xs hover:bg-zinc-50 disabled:opacity-40"
                >
                  {t("Pagination.Next")}
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
