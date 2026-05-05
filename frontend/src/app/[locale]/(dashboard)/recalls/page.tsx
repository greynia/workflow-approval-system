"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { RotateCcw } from "lucide-react";
import RecallService from "@/services/recall.service";
import { useToastStore } from "@/stores/toast-store";
import type { PendingRecall } from "@/types/recall";
import { useFormatDurationAsHours } from "@/lib/use-format-duration";
import { PageHeader } from "@/components/ui/page-header";
import { DataTable, type ColumnDef } from "@/components/ui/data-table";
import { EmptyState } from "@/components/ui/empty-state";
import { Button } from "@/components/ui/button";
import { ApprovalDialog } from "@/components/approval/approval-dialog";

function formatDate(date: string): string {
  return new Date(date).toLocaleString();
}

export default function RecallsPage() {
  const t = useTranslations("Recalls");
  const formatDuration = useFormatDurationAsHours();
  const toast = useToastStore();
  const queryClient = useQueryClient();

  const [approveTarget, setApproveTarget] = useState<PendingRecall | null>(null);
  const [rejectTarget, setRejectTarget] = useState<PendingRecall | null>(null);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["recalls", "pending"],
    queryFn: RecallService.getPendingList,
  });

  function invalidateAfterAction() {
    queryClient.invalidateQueries({ queryKey: ["recalls", "pending"] });
    queryClient.invalidateQueries({ queryKey: ["recalls", "pending", "count"] });
    queryClient.invalidateQueries({ queryKey: ["requests"] });
  }

  const approveMutation = useMutation({
    mutationFn: ({ stepId }: { stepId: number }) => RecallService.approve(stepId),
    onSuccess: () => {
      invalidateAfterAction();
      toast.success(t("ApproveSuccess"));
      setApproveTarget(null);
    },
    onError: () => {
      toast.error(t("ActionError"));
    },
  });

  const rejectMutation = useMutation({
    mutationFn: ({ stepId, comment }: { stepId: number; comment: string }) =>
      RecallService.reject(stepId, { comment }),
    onSuccess: () => {
      invalidateAfterAction();
      toast.success(t("RejectSuccess"));
      setRejectTarget(null);
    },
    onError: () => {
      toast.error(t("ActionError"));
    },
  });

  const columns: ColumnDef<PendingRecall>[] = [
    {
      id: "applicant",
      header: t("Table.Applicant"),
      cell: (row) => (
        <span className="font-medium text-foreground">{row.applicantName}</span>
      ),
    },
    {
      id: "leaveType",
      header: t("Table.LeaveType"),
      cell: (row) => t(`LeaveType.${row.leaveType}`),
      hideOnMobile: true,
    },
    {
      id: "dateRange",
      header: t("Table.DateRange"),
      cell: (row) => `${formatDate(row.startTime)} – ${formatDate(row.endTime)}`,
      hideOnMobile: true,
    },
    {
      id: "duration",
      header: t("Table.Duration"),
      cell: (row) => formatDuration(row.durationMinutes),
      hideOnMobile: true,
    },
    {
      id: "recallReason",
      header: t("Table.RecallReason"),
      cell: (row) => (
        <span className="max-w-[200px] truncate text-muted-foreground">{row.recallReason}</span>
      ),
    },
  ];

  const renderActions = (row: PendingRecall) => (
    <div className="flex justify-end gap-2">
      <Button size="sm" variant="success" onClick={() => setApproveTarget(row)}>
        {t("ApproveButton")}
      </Button>
      <Button size="sm" variant="destructive" onClick={() => setRejectTarget(row)}>
        {t("RejectButton")}
      </Button>
    </div>
  );

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-6">
      <PageHeader title={t("Title")} description={t("Description")} />

      {isError ? (
        <EmptyState title={t("Error")} />
      ) : (
        <DataTable
          columns={columns}
          data={data ?? []}
          keyField={(row) => row.stepId}
          loading={isLoading}
          getRowHref={(row) => `/recalls/${row.requestId}`}
          rowActions={renderActions}
          emptyState={<EmptyState icon={<RotateCcw />} title={t("Empty")} />}
          renderMobileCard={(row) => (
            <div className="rounded-lg border border-border bg-card p-4">
              <div className="flex items-start justify-between gap-2">
                <span className="font-medium text-foreground">{row.applicantName}</span>
                <span className="shrink-0 text-sm text-muted-foreground">
                  {t(`LeaveType.${row.leaveType}`)}
                </span>
              </div>
              <p className="mt-1 text-sm text-muted-foreground">
                {formatDate(row.startTime)} – {formatDate(row.endTime)}
              </p>
              <p className="mt-0.5 text-xs text-muted-foreground">
                {formatDuration(row.durationMinutes)}
              </p>
              <p className="mt-1 text-sm text-muted-foreground">{row.recallReason}</p>
              <div className="mt-3 flex gap-2">
                <Button
                  size="sm"
                  variant="success"
                  className="flex-1"
                  onClick={() => setApproveTarget(row)}
                >
                  {t("ApproveButton")}
                </Button>
                <Button
                  size="sm"
                  variant="destructive"
                  className="flex-1"
                  onClick={() => setRejectTarget(row)}
                >
                  {t("RejectButton")}
                </Button>
              </div>
            </div>
          )}
        />
      )}

      {approveTarget ? (
        <ApprovalDialog
          variant="approve"
          open
          onOpenChange={(open) => !open && setApproveTarget(null)}
          title={t("ApproveDialog.Title")}
          description={t("ApproveDialog.Description")}
          applicantSummary={
            <>
              {approveTarget.applicantName} — {formatDate(approveTarget.startTime)} ～{" "}
              {formatDate(approveTarget.endTime)}
            </>
          }
          confirmLabel={t("ApproveDialog.Confirm")}
          cancelLabel={t("ApproveDialog.Cancel")}
          isPending={approveMutation.isPending}
          onConfirm={() => approveMutation.mutate({ stepId: approveTarget.stepId })}
        />
      ) : null}

      {rejectTarget ? (
        <ApprovalDialog
          variant="reject"
          open
          onOpenChange={(open) => !open && setRejectTarget(null)}
          title={t("RejectDialog.Title")}
          description={t("RejectDialog.Description")}
          applicantSummary={
            <>
              {rejectTarget.applicantName} — {formatDate(rejectTarget.startTime)} ～{" "}
              {formatDate(rejectTarget.endTime)}
            </>
          }
          commentLabel={t("RejectDialog.CommentLabel")}
          commentPlaceholder={t("RejectDialog.CommentPlaceholder")}
          commentRequiredError={t("RejectDialog.CommentRequired")}
          confirmLabel={t("RejectDialog.Confirm")}
          cancelLabel={t("RejectDialog.Cancel")}
          isPending={rejectMutation.isPending}
          onConfirm={(comment) =>
            rejectMutation.mutate({ stepId: rejectTarget.stepId, comment: comment ?? "" })
          }
        />
      ) : null}
    </div>
  );
}
