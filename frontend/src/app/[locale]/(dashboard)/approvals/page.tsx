"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Inbox } from "lucide-react";
import ApprovalService from "@/services/approval.service";
import { useToastStore } from "@/stores/toast-store";
import type { PendingApproval } from "@/types/approval";
import { useFormatDurationAsHours } from "@/lib/use-format-duration";
import { PageHeader } from "@/components/ui/page-header";
import { DataTable, type ColumnDef } from "@/components/ui/data-table";
import { EmptyState } from "@/components/ui/empty-state";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ApprovalDialog } from "@/components/approval/approval-dialog";

function isDeputyStep(row: { stepType: PendingApproval["stepType"] }): boolean {
  return row.stepType === "DEPUTY";
}

function formatDate(date: string): string {
  return new Date(date).toLocaleString();
}

export default function ApprovalsPage() {
  const t = useTranslations("Approvals.List");
  const formatDuration = useFormatDurationAsHours();
  const toast = useToastStore();
  const queryClient = useQueryClient();

  const [approveTarget, setApproveTarget] = useState<PendingApproval | null>(null);
  const [rejectTarget, setRejectTarget] = useState<PendingApproval | null>(null);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["approvals", "pending"],
    queryFn: ApprovalService.getPendingList,
  });

  function invalidateAfterAction() {
    queryClient.invalidateQueries({ queryKey: ["approvals", "pending"] });
    queryClient.invalidateQueries({ queryKey: ["approvals", "pending", "count"] });
    queryClient.invalidateQueries({ queryKey: ["requests"] });
  }

  const approveMutation = useMutation({
    mutationFn: ({ stepId }: { stepId: number; isDeputy: boolean }) =>
      ApprovalService.approve(stepId, {}),
    onSuccess: (_data, variables) => {
      invalidateAfterAction();
      toast.success(
        t(variables.isDeputy ? "DeputyApproveSuccess" : "ApproveSuccess"),
      );
      setApproveTarget(null);
    },
    onError: () => {
      toast.error(t("ActionError"));
    },
  });

  const rejectMutation = useMutation({
    mutationFn: ({
      stepId,
      comment,
    }: {
      stepId: number;
      comment: string;
      isDeputy: boolean;
    }) => ApprovalService.reject(stepId, { comment }),
    onSuccess: (_data, variables) => {
      invalidateAfterAction();
      toast.success(
        t(variables.isDeputy ? "DeputyRejectSuccess" : "RejectSuccess"),
      );
      setRejectTarget(null);
    },
    onError: () => {
      toast.error(t("ActionError"));
    },
  });

  const columns: ColumnDef<PendingApproval>[] = [
    {
      id: "applicant",
      header: t("Table.Applicant"),
      cell: (row) => (
        <span className="font-medium text-foreground">{row.applicantName}</span>
      ),
    },
    {
      id: "step",
      header: t("Table.Step"),
      cell: (row) => (
        <Badge variant={isDeputyStep(row) ? "secondary" : "default"}>
          {t(`StepLabel.${row.stepType}`)}
        </Badge>
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
      header: t("Table.Days"),
      cell: (row) => formatDuration(row.durationMinutes),
      hideOnMobile: true,
    },
  ];

  const renderActions = (row: PendingApproval) => {
    const deputy = isDeputyStep(row);
    return (
      <div className="flex justify-end gap-2">
        <Button
          size="sm"
          variant="success"
          onClick={() => setApproveTarget(row)}
        >
          {t(deputy ? "DeputyApproveButton" : "ApproveButton")}
        </Button>
        <Button
          size="sm"
          variant="destructive"
          onClick={() => setRejectTarget(row)}
        >
          {t(deputy ? "DeputyRejectButton" : "RejectButton")}
        </Button>
      </div>
    );
  };

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
          getRowHref={(row) => `/approvals/${row.requestId}`}
          rowActions={renderActions}
          emptyState={<EmptyState icon={<Inbox />} title={t("Empty")} />}
          renderMobileCard={(row) => (
            <div className="rounded-lg border border-border bg-card p-4">
              <div className="flex items-start justify-between gap-2">
                <span className="font-medium text-foreground">
                  {row.applicantName}
                </span>
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
              <div className="mt-2 flex items-center gap-2">
                <Badge variant={isDeputyStep(row) ? "secondary" : "default"}>
                  {t(`StepLabel.${row.stepType}`)}
                </Badge>
              </div>
              <div className="mt-3 flex gap-2">
                <Button
                  size="sm"
                  variant="success"
                  className="flex-1"
                  onClick={() => setApproveTarget(row)}
                >
                  {t(isDeputyStep(row) ? "DeputyApproveButton" : "ApproveButton")}
                </Button>
                <Button
                  size="sm"
                  variant="destructive"
                  className="flex-1"
                  onClick={() => setRejectTarget(row)}
                >
                  {t(isDeputyStep(row) ? "DeputyRejectButton" : "RejectButton")}
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
          title={t(
            isDeputyStep(approveTarget)
              ? "DeputyApproveDialog.Title"
              : "ApproveDialog.Title",
          )}
          description={t(
            isDeputyStep(approveTarget)
              ? "DeputyApproveDialog.Description"
              : "ApproveDialog.Description",
          )}
          applicantSummary={
            <>
              {approveTarget.applicantName} — {formatDate(approveTarget.startTime)} ～{" "}
              {formatDate(approveTarget.endTime)}
            </>
          }
          confirmLabel={t(
            isDeputyStep(approveTarget)
              ? "DeputyApproveDialog.Confirm"
              : "ApproveDialog.Confirm",
          )}
          cancelLabel={t("ApproveDialog.Cancel")}
          isPending={approveMutation.isPending}
          onConfirm={() =>
            approveMutation.mutate({
              stepId: approveTarget.stepId,
              isDeputy: isDeputyStep(approveTarget),
            })
          }
        />
      ) : null}

      {rejectTarget ? (
        <ApprovalDialog
          variant="reject"
          open
          onOpenChange={(open) => !open && setRejectTarget(null)}
          title={t(
            isDeputyStep(rejectTarget)
              ? "DeputyRejectDialog.Title"
              : "RejectDialog.Title",
          )}
          description={t(
            isDeputyStep(rejectTarget)
              ? "DeputyRejectDialog.Description"
              : "RejectDialog.Description",
          )}
          applicantSummary={
            <>
              {rejectTarget.applicantName} — {formatDate(rejectTarget.startTime)} ～{" "}
              {formatDate(rejectTarget.endTime)}
            </>
          }
          commentLabel={t(
            isDeputyStep(rejectTarget)
              ? "DeputyRejectDialog.CommentLabel"
              : "RejectDialog.CommentLabel",
          )}
          commentPlaceholder={t(
            isDeputyStep(rejectTarget)
              ? "DeputyRejectDialog.CommentPlaceholder"
              : "RejectDialog.CommentPlaceholder",
          )}
          commentRequiredError={t(
            isDeputyStep(rejectTarget)
              ? "DeputyRejectDialog.CommentRequired"
              : "RejectDialog.CommentRequired",
          )}
          confirmLabel={t(
            isDeputyStep(rejectTarget)
              ? "DeputyRejectDialog.Confirm"
              : "RejectDialog.Confirm",
          )}
          cancelLabel={t("RejectDialog.Cancel")}
          isPending={rejectMutation.isPending}
          onConfirm={(comment) =>
            rejectMutation.mutate({
              stepId: rejectTarget.stepId,
              comment: comment ?? "",
              isDeputy: isDeputyStep(rejectTarget),
            })
          }
        />
      ) : null}
    </div>
  );
}
