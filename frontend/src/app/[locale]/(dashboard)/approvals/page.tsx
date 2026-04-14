"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import ApprovalService from "@/services/approval.service";
import type { PendingApproval } from "@/types/approval";

function formatDate(date: string): string {
  return new Date(`${date}T00:00:00`).toLocaleDateString();
}

export default function ApprovalsPage() {
  const t = useTranslations("Approvals.List");
  const { data, isLoading, isError } = useQuery({
    queryKey: ["approvals", "pending"],
    queryFn: ApprovalService.getPendingList,
  });

  return (
    <div className="mx-auto max-w-6xl">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold text-zinc-900">{t("Title")}</h1>
          <p className="mt-1 text-sm text-zinc-500">{t("Description")}</p>
        </div>
      </div>

      {isLoading && <LoadingSkeleton />}
      {isError && (
        <p className="py-10 text-center text-sm text-zinc-500">{t("Error")}</p>
      )}
      {!isLoading && !isError && data && <ApprovalsTable items={data} />}
    </div>
  );
}

function LoadingSkeleton() {
  return (
    <div className="overflow-hidden rounded-lg border border-zinc-200 bg-white">
      <table className="w-full text-sm">
        <tbody>
          {Array.from({ length: 4 }).map((_, i) => (
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

function ApprovalsTable({ items }: { items: PendingApproval[] }) {
  const t = useTranslations("Approvals.List");

  if (items.length === 0) {
    return <p className="py-16 text-center text-sm text-zinc-400">{t("Empty")}</p>;
  }

  return (
    <div className="overflow-hidden rounded-lg border border-zinc-200 bg-white">
      <table className="w-full text-sm">
        <thead className="border-b border-zinc-200 bg-zinc-50">
          <tr>
            <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-zinc-500">
              {t("Table.Applicant")}
            </th>
            <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-zinc-500">
              {t("Table.LeaveType")}
            </th>
            <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-zinc-500">
              {t("Table.DateRange")}
            </th>
            <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-zinc-500">
              {t("Table.Days")}
            </th>
            <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-zinc-500">
              {t("Table.Step")}
            </th>
          </tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <tr
              key={item.stepId}
              className="border-b border-zinc-100 transition-colors last:border-0 hover:bg-zinc-50"
            >
              <td className="px-4 py-3 font-medium text-zinc-900">
                <Link
                  href={`/requests/${item.requestId}`}
                  className="transition-colors hover:text-zinc-600"
                >
                  {item.applicantName}
                </Link>
              </td>
              <td className="px-4 py-3 text-zinc-600">
                {t(`LeaveType.${item.leaveType}`)}
              </td>
              <td className="px-4 py-3 text-zinc-600">
                {formatDate(item.startDate)} - {formatDate(item.endDate)}
              </td>
              <td className="px-4 py-3 text-zinc-600">{item.days}</td>
              <td className="px-4 py-3 text-zinc-600">
                {t("StepLabel", { step: item.stepOrder })}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
