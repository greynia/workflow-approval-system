"use client";

import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import LeaveService from "@/services/leave.service";
import { useFormatDurationAsHours } from "@/lib/use-format-duration";

export default function LeaveBalancesPage() {
  const t = useTranslations("Balances");
  const tLeaveType = useTranslations("Requests.List.LeaveType");
  const formatDuration = useFormatDurationAsHours();
  const year = useMemo(() => new Date().getFullYear(), []);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["requests", "balance", year],
    queryFn: () => LeaveService.getBalances(year),
  });

  return (
    <div className="mx-auto max-w-5xl">
      <div className="mb-6">
        <h1 className="text-xl font-semibold text-zinc-900">{t("Title")}</h1>
        <p className="mt-1 text-sm text-zinc-500">{t("Description", { year })}</p>
      </div>

      {isLoading ? (
        <div className="overflow-x-auto rounded-lg border border-zinc-200 bg-white">
          <table className="w-full text-sm">
            <tbody>
              {Array.from({ length: 4 }).map((_, index) => (
                <tr key={index} className="border-b border-zinc-100 last:border-0">
                  {Array.from({ length: 4 }).map((__, col) => (
                    <td key={col} className="px-4 py-4">
                      <div className="h-4 animate-pulse rounded bg-zinc-100" />
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}

      {isError ? (
        <p className="py-10 text-center text-sm text-zinc-500">{t("Error")}</p>
      ) : null}

      {!isLoading && !isError && data ? (
        <>
          {/* Mobile card list */}
          <div className="space-y-3 md:hidden">
            {data.map((item) => (
              <div key={item.leaveType} className="rounded-lg border border-zinc-200 bg-white p-4">
                <p className="font-medium text-zinc-900">{tLeaveType(item.leaveType)}</p>
                <div className="mt-3 grid grid-cols-3 gap-2 text-center">
                  <div>
                    <p className="text-xs text-zinc-400">{t("Table.Quota")}</p>
                    <p className="mt-0.5 text-sm font-medium text-zinc-900">{formatDuration(item.quotaMinutes)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-zinc-400">{t("Table.Used")}</p>
                    <p className="mt-0.5 text-sm font-medium text-zinc-900">{formatDuration(item.usedMinutes)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-zinc-400">{t("Table.Remaining")}</p>
                    <p className="mt-0.5 text-sm font-medium text-zinc-800">{formatDuration(item.remainingMinutes)}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Desktop table */}
          <div className="hidden overflow-x-auto rounded-lg border border-zinc-200 bg-white md:block">
            <table className="w-full text-sm">
              <thead className="border-b border-zinc-200 bg-zinc-50">
                <tr>
                  {(["LeaveType", "Quota", "Used", "Remaining"] as const).map((col) => (
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
                {data.map((item) => (
                  <tr
                    key={item.leaveType}
                    className="border-b border-zinc-100 transition-colors last:border-0 hover:bg-zinc-50"
                  >
                    <td className="px-4 py-4 font-medium text-zinc-900">
                      {tLeaveType(item.leaveType)}
                    </td>
                    <td className="px-4 py-4 text-zinc-600">
                      {formatDuration(item.quotaMinutes)}
                    </td>
                    <td className="px-4 py-4 text-zinc-600">
                      {formatDuration(item.usedMinutes)}
                    </td>
                    <td className="px-4 py-4">
                      <span className="rounded-full bg-zinc-100 px-3 py-1 text-sm font-medium text-zinc-800">
                        {formatDuration(item.remainingMinutes)}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      ) : null}
    </div>
  );
}
