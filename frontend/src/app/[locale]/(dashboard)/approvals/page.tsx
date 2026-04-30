"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import * as Dialog from "@radix-ui/react-dialog";
import { useTranslations } from "next-intl";
import { useRouter } from "@/i18n/navigation";
import ApprovalService from "@/services/approval.service";
import { useToastStore } from "@/stores/toast-store";
import type { PendingApproval } from "@/types/approval";
import { useFormatDurationAsHours } from "@/lib/use-format-duration";

function formatDate(date: string): string {
  return new Date(date).toLocaleString();
}

function ApproveDialog({
  target,
  onClose,
}: {
  target: PendingApproval;
  onClose: () => void;
}) {
  const t = useTranslations("Approvals.List");
  const toast = useToastStore();
  const queryClient = useQueryClient();

  const approveMutation = useMutation({
    mutationFn: () => ApprovalService.approve(target.stepId, {}),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["approvals", "pending"] });
      queryClient.invalidateQueries({ queryKey: ["approvals", "pending", "count"] });
      // Invalidate all request detail caches so the timeline reflects the new approval step
      queryClient.invalidateQueries({ queryKey: ["requests"] });
      toast.success(t("ApproveSuccess"));
      onClose();
    },
    onError: () => {
      toast.error(t("ActionError"));
    },
  });

  return (
    <Dialog.Root open onOpenChange={(open) => !open && onClose()}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/40" />
        <Dialog.Content className="fixed left-1/2 top-1/2 w-full max-w-sm -translate-x-1/2 -translate-y-1/2 rounded-lg bg-white p-6 shadow-lg focus:outline-none">
          <Dialog.Title className="text-base font-semibold text-zinc-900">{t("ApproveDialog.Title")}</Dialog.Title>
          <Dialog.Description className="mt-2 text-sm text-zinc-500">{t("ApproveDialog.Description")}</Dialog.Description>
          <p className="mt-3 text-sm font-medium text-zinc-800">
            {target.applicantName} — {formatDate(target.startTime)} ～ {formatDate(target.endTime)}
          </p>
          <div className="mt-5 flex justify-end gap-2">
            <button onClick={onClose} disabled={approveMutation.isPending} className="rounded-md px-4 py-2 text-sm text-zinc-600 hover:bg-zinc-50 disabled:opacity-50">
              {t("ApproveDialog.Cancel")}
            </button>
            <button onClick={() => approveMutation.mutate()} disabled={approveMutation.isPending} className="rounded-md bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50">
              {approveMutation.isPending ? "…" : t("ApproveDialog.Confirm")}
            </button>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

function RejectDialog({
  target,
  onClose,
}: {
  target: PendingApproval;
  onClose: () => void;
}) {
  const t = useTranslations("Approvals.List");
  const toast = useToastStore();
  const queryClient = useQueryClient();
  const [comment, setComment] = useState("");
  const [commentError, setCommentError] = useState("");

  const rejectMutation = useMutation({
    mutationFn: ({ comment }: { comment: string }) => ApprovalService.reject(target.stepId, { comment }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["approvals", "pending"] });
      queryClient.invalidateQueries({ queryKey: ["approvals", "pending", "count"] });
      // Invalidate all request detail caches so the timeline reflects the rejection
      queryClient.invalidateQueries({ queryKey: ["requests"] });
      toast.success(t("RejectSuccess"));
      onClose();
    },
    onError: () => {
      toast.error(t("ActionError"));
    },
  });

  function handleSubmit() {
    const trimmedComment = comment.trim();
    if (!trimmedComment) {
      setCommentError(t("RejectDialog.CommentRequired"));
      return;
    }
    rejectMutation.mutate({ comment: trimmedComment });
  }

  return (
    <Dialog.Root open onOpenChange={(open) => !open && onClose()}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/40" />
        <Dialog.Content className="fixed left-1/2 top-1/2 w-full max-w-sm -translate-x-1/2 -translate-y-1/2 rounded-lg bg-white p-6 shadow-lg focus:outline-none">
          <Dialog.Title className="text-base font-semibold text-zinc-900">{t("RejectDialog.Title")}</Dialog.Title>
          <Dialog.Description className="mt-1 text-sm text-zinc-500">{t("RejectDialog.Description")}</Dialog.Description>
          <div className="mt-4">
            <label className="block text-sm font-medium text-zinc-700">{t("RejectDialog.CommentLabel")}</label>
            <textarea
              className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm placeholder-zinc-400 focus:border-zinc-500 focus:outline-none"
              rows={3}
              placeholder={t("RejectDialog.CommentPlaceholder")}
              value={comment}
              onChange={(e) => {
                setComment(e.target.value);
                if (commentError) setCommentError("");
              }}
            />
            {commentError && <p className="mt-1 text-xs text-red-500">{commentError}</p>}
          </div>
          <div className="mt-4 flex justify-end gap-2">
            <button onClick={onClose} disabled={rejectMutation.isPending} className="rounded-md px-4 py-2 text-sm text-zinc-600 hover:bg-zinc-50 disabled:opacity-50">
              {t("RejectDialog.Cancel")}
            </button>
            <button onClick={handleSubmit} disabled={rejectMutation.isPending} className="rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50">
              {rejectMutation.isPending ? "…" : t("RejectDialog.Confirm")}
            </button>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
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

function ApprovalsTable({
  items,
  onApprove,
  onReject,
}: {
  items: PendingApproval[];
  onApprove: (item: PendingApproval) => void;
  onReject: (item: PendingApproval) => void;
}) {
  const t = useTranslations("Approvals.List");
  const formatDuration = useFormatDurationAsHours();
  const router = useRouter();

  if (items.length === 0) {
    return <p className="py-16 text-center text-sm text-zinc-400">{t("Empty")}</p>;
  }

  return (
    <>
      {/* Desktop table */}
      <div className="hidden overflow-hidden rounded-lg border border-zinc-200 bg-white md:block">
        <table className="w-full text-sm">
          <thead className="border-b border-zinc-200 bg-zinc-50">
            <tr>
              {(["Applicant", "LeaveType", "DateRange", "Days", "Actions"] as const).map((col) => (
                <th key={col} className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-zinc-500">
                  {t(`Table.${col}`)}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr
                key={item.stepId}
                className="cursor-pointer border-b border-zinc-100 transition-colors last:border-0 hover:bg-zinc-50"
                onClick={() => router.push(`/approvals/${item.requestId}`)}
              >
                <td className="px-4 py-3 font-medium text-zinc-900">
                  {item.applicantName}
                </td>
                <td className="px-4 py-3 text-zinc-600">{t(`LeaveType.${item.leaveType}`)}</td>
                <td className="px-4 py-3 text-zinc-600">{formatDate(item.startTime)} – {formatDate(item.endTime)}</td>
                <td className="px-4 py-3 text-zinc-600">{formatDuration(item.durationMinutes)}</td>
                <td className="px-4 py-3">
                  <div className="flex gap-2">
                    <button
                      onClick={(e) => { e.stopPropagation(); onApprove(item); }}
                      className="rounded-md bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-700 hover:bg-emerald-100"
                    >
                      {t("ApproveButton")}
                    </button>
                    <button
                      onClick={(e) => { e.stopPropagation(); onReject(item); }}
                      className="rounded-md bg-red-50 px-3 py-1 text-xs font-medium text-red-700 hover:bg-red-100"
                    >
                      {t("RejectButton")}
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Mobile card list */}
      <div className="space-y-3 md:hidden">
        {items.map((item) => (
          <div
            key={item.stepId}
            className="cursor-pointer rounded-lg border border-zinc-200 bg-white p-4 transition-colors hover:bg-zinc-50"
            onClick={() => router.push(`/approvals/${item.requestId}`)}
          >
            <div className="flex items-start justify-between gap-2">
              <span className="font-medium text-zinc-900">{item.applicantName}</span>
              <span className="shrink-0 text-sm text-zinc-500">{t(`LeaveType.${item.leaveType}`)}</span>
            </div>
            <p className="mt-1 text-sm text-zinc-500">
              {formatDate(item.startTime)} – {formatDate(item.endTime)}
            </p>
            <p className="mt-0.5 text-xs text-zinc-400">{formatDuration(item.durationMinutes)}</p>
            <div className="mt-3 flex gap-2">
              <button
                onClick={(e) => { e.stopPropagation(); onApprove(item); }}
                className="flex-1 rounded-md bg-emerald-50 py-2 text-sm font-medium text-emerald-700 hover:bg-emerald-100"
              >
                {t("ApproveButton")}
              </button>
              <button
                onClick={(e) => { e.stopPropagation(); onReject(item); }}
                className="flex-1 rounded-md bg-red-50 py-2 text-sm font-medium text-red-700 hover:bg-red-100"
              >
                {t("RejectButton")}
              </button>
            </div>
          </div>
        ))}
      </div>
    </>
  );
}

export default function ApprovalsPage() {
  const t = useTranslations("Approvals.List");
  const [approveTarget, setApproveTarget] = useState<PendingApproval | null>(null);
  const [rejectTarget, setRejectTarget] = useState<PendingApproval | null>(null);

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
      {isError && <p className="py-10 text-center text-sm text-zinc-500">{t("Error")}</p>}
      {!isLoading && !isError && data && (
        <ApprovalsTable items={data} onApprove={setApproveTarget} onReject={setRejectTarget} />
      )}

      {approveTarget && <ApproveDialog target={approveTarget} onClose={() => setApproveTarget(null)} />}
      {rejectTarget && <RejectDialog target={rejectTarget} onClose={() => setRejectTarget(null)} />}
    </div>
  );
}
