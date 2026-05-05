"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { FileX, Plus } from "lucide-react";
import { Link } from "@/i18n/navigation";
import LeaveService from "@/services/leave.service";
import { appConfig } from "@/configs/app.config";
import type { LeaveRequestSummary, LeaveType, RequestStatus } from "@/types/leave";
import type { PageResponse } from "@/types/common";
import { useFormatDurationAsHours } from "@/lib/use-format-duration";
import { PageHeader } from "@/components/ui/page-header";
import { DataTable, type ColumnDef } from "@/components/ui/data-table";
import { StatusBadge } from "@/components/ui/status-badge";
import { EmptyState } from "@/components/ui/empty-state";
import { Button } from "@/components/ui/button";

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleString();
}

export default function RequestsPage() {
  const t = useTranslations("Requests.List");
  const formatDuration = useFormatDurationAsHours();

  const { data, isLoading, isError } = useQuery<PageResponse<LeaveRequestSummary>>({
    queryKey: ["requests"],
    queryFn: () => LeaveService.getList(0, 10),
  });

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
    PENDING_RECALL: t("StatusLabel.PENDING_RECALL"),
  };

  const columns: ColumnDef<LeaveRequestSummary>[] = [
    {
      id: "type",
      header: t("Table.Type"),
      cell: (row) => (
        <span className="font-medium text-foreground">
          {leaveTypeLabel[row.type]}
        </span>
      ),
    },
    {
      id: "dateRange",
      header: t("Table.DateRange"),
      cell: (row) => `${formatDate(row.startTime)} – ${formatDate(row.endTime)}`,
      hideOnMobile: true,
    },
    {
      id: "duration",
      header: t("Table.Days"),
      cell: (row) => formatDuration(row.durationMinutes),
      hideOnMobile: true,
    },
    {
      id: "status",
      header: t("Table.Status"),
      cell: (row) => (
        <StatusBadge status={row.status} label={statusLabel[row.status]} />
      ),
    },
    {
      id: "createdAt",
      header: t("Table.CreatedAt"),
      cell: (row) => (
        <span className="text-muted-foreground">
          {new Date(row.createdAt).toLocaleString()}
        </span>
      ),
      hideOnMobile: true,
    },
  ];

  const newRequestButton = (
    <Button asChild>
      <Link href={appConfig.routes.requestsNew}>
        <Plus aria-hidden />
        {t("NewButton")}
      </Link>
    </Button>
  );

  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-6">
      <PageHeader title={t("Title")} actions={newRequestButton} />

      {isError ? (
        <EmptyState title={t("Error")} />
      ) : (
        <DataTable
          columns={columns}
          data={data?.items ?? []}
          keyField={(row) => row.id}
          loading={isLoading}
          getRowHref={(row) => `/requests/${row.id}`}
          emptyState={
            <EmptyState
              icon={<FileX />}
              title={t("Empty")}
              action={newRequestButton}
            />
          }
          renderMobileCard={(row) => (
            <Link
              href={`/requests/${row.id}`}
              className="block rounded-lg border border-border bg-card p-4 transition-colors hover:bg-muted/50"
            >
              <div className="flex items-start justify-between gap-2">
                <span className="font-medium text-foreground">
                  {leaveTypeLabel[row.type]}
                </span>
                <StatusBadge status={row.status} label={statusLabel[row.status]} />
              </div>
              <p className="mt-1 text-sm text-muted-foreground">
                {formatDate(row.startTime)} – {formatDate(row.endTime)}
              </p>
              <div className="mt-2 flex items-center justify-between text-xs text-muted-foreground">
                <span>{formatDuration(row.durationMinutes)}</span>
                <span>{new Date(row.createdAt).toLocaleString()}</span>
              </div>
            </Link>
          )}
        />
      )}
    </div>
  );
}
