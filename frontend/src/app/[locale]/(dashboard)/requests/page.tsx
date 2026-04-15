"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import LeaveService from "@/services/leave.service";
import { appConfig } from "@/configs/app.config";
import type { LeaveRequestSummary, LeaveType, RequestStatus } from "@/types/leave";
import type { PageResponse } from "@/types/common";
import { useFormatDurationAsHours } from "@/lib/use-format-duration";

const STATUS_STYLES: Record<RequestStatus, string> = {
  PENDING: "bg-amber-100 text-amber-700",
  APPROVED: "bg-green-100 text-green-700",
  REJECTED: "bg-red-100 text-red-700",
  CANCELLED: "bg-zinc-100 text-zinc-500",
};

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleString();
}

export default function RequestsPage() {
  const t = useTranslations("Requests.List");

  const { data, isLoading, isError } = useQuery<PageResponse<LeaveRequestSummary>>({
    queryKey: ["requests"],
    queryFn: () => LeaveService.getList(0, 10),
  });

  return (
    <div className="mx-auto max-w-5xl">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-zinc-900">{t("Title")}</h1>
        <Link
          href={appConfig.routes.requestsNew}
          className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-zinc-700"
        >
          {t("NewButton")}
        </Link>
      </div>

      {isLoading && <LoadingSkeleton />}
      {isError && <p className="py-10 text-center text-sm text-zinc-500">{t("Error")}</p>}
      {!isLoading && !isError && data && <RequestsTable items={data.items} t={t} />}
    </div>
  );
}

function LoadingSkeleton() {
  return (
    <div className="overflow-hidden rounded-lg border border-zinc-200 bg-white">
      <table className="w-full text-sm">
        <tbody>
          {Array.from({ length: 5 }).map((_, i) => (
            <tr key={i} className="border-b border-zinc-100 last:border-0">
              {Array.from({ length: 5 }).map((_, j) => (
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

type ListTranslations = ReturnType<typeof useTranslations<"Requests.List">>;

function RequestsTable({
  items,
  t,
}: {
  items: LeaveRequestSummary[];
  t: ListTranslations;
}) {
  const formatDuration = useFormatDurationAsHours();
  if (items.length === 0) {
    return <p className="py-16 text-center text-sm text-zinc-400">{t("Empty")}</p>;
  }

  const leaveTypeLabel: Record<LeaveType, string> = {
    ANNUAL: t("LeaveType.ANNUAL"),
    SICK: t("LeaveType.SICK"),
    PERSONAL: t("LeaveType.PERSONAL"),
    OTHER: t("LeaveType.OTHER"),
  };

  const statusLabel: Record<RequestStatus, string> = {
    PENDING: t("StatusLabel.PENDING"),
    APPROVED: t("StatusLabel.APPROVED"),
    REJECTED: t("StatusLabel.REJECTED"),
    CANCELLED: t("StatusLabel.CANCELLED"),
  };

  return (
    <div className="overflow-hidden rounded-lg border border-zinc-200 bg-white">
      <table className="w-full text-sm">
        <thead className="border-b border-zinc-200 bg-zinc-50">
          <tr>
            <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-zinc-500">{t("Table.Type")}</th>
            <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-zinc-500">{t("Table.DateRange")}</th>
            <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-zinc-500">{t("Table.Days")}</th>
            <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-zinc-500">{t("Table.Status")}</th>
            <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-zinc-500">{t("Table.CreatedAt")}</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <tr key={item.id} className="border-b border-zinc-100 transition-colors last:border-0 hover:bg-zinc-50">
              <td className="px-4 py-3 font-medium text-zinc-900">
                <Link href={`/requests/${item.id}`} className="transition-colors hover:text-zinc-600">
                  {leaveTypeLabel[item.type]}
                </Link>
              </td>
              <td className="px-4 py-3 text-zinc-600">{formatDate(item.startTime)} – {formatDate(item.endTime)}</td>
              <td className="px-4 py-3 text-zinc-600">{formatDuration(item.durationMinutes)}</td>
              <td className="px-4 py-3">
                <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_STYLES[item.status]}`}>
                  {statusLabel[item.status]}
                </span>
              </td>
              <td className="px-4 py-3 text-zinc-500">{new Date(item.createdAt).toLocaleString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
