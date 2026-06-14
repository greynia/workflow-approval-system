"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useParams } from "next/navigation";
import { usePathname } from "@/i18n/navigation";
import LeaveService from "@/services/leave.service";
import { useFormatDurationAsHours } from "@/lib/use-format-duration";
import { useAuth } from "@/hooks/useAuth";
import { useAuthority } from "@/hooks/useAuthority";
import { useToastStore } from "@/stores/toast-store";
import { PageHeader } from "@/components/ui/page-header";
import { Button } from "@/components/ui/button";
import { Section } from "@/components/ui/section";
import {
  DescriptionList,
  DescriptionItem,
} from "@/components/ui/description-list";
import { StatusBadge } from "@/components/ui/status-badge";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  RequestTimeline,
  type RequestTimelineLabels,
} from "@/components/leave/request-timeline";
import { AiReviewSection } from "./_components/ai-review-section";
import type { ApprovalStepType, StepStatus } from "@/types/leave";

function formatDate(date: string): string {
  return new Date(date).toLocaleString();
}

export default function RequestDetailPage() {
  const params = useParams<{ id: string }>();
  const t = useTranslations("Requests.Detail");
  const formatDuration = useFormatDurationAsHours();
  const pathname = usePathname();
  const requestId = Number(params.id);
  const isApprovalContext = pathname.startsWith("/approvals/");
  const isRecallContext = pathname.startsWith("/recalls/");
  const backHref = isRecallContext ? "/recalls" : isApprovalContext ? "/approvals" : "/requests";
  const backLabel = isRecallContext ? t("BackToRecalls") : isApprovalContext ? t("BackToApprovals") : t("BackToRequests");
  const { user } = useAuth();
  const toast = useToastStore();
  const queryClient = useQueryClient();
  const canViewAiReview = useAuthority(user?.permissions ?? [], ["approval.view"]);
  const [showRecallDialog, setShowRecallDialog] = useState(false);
  const [recallReason, setRecallReason] = useState("");

  const { data, isLoading, isError } = useQuery({
    queryKey: ["requests", requestId],
    queryFn: () => LeaveService.getDetail(requestId),
    enabled: Number.isInteger(requestId) && requestId > 0,
  });

  const recallMutation = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) =>
      LeaveService.recall(id, { reason }),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ["requests", variables.id] });
      queryClient.invalidateQueries({ queryKey: ["requests"] });
      toast.success(t("RecallSuccess"));
      setShowRecallDialog(false);
      setRecallReason("");
    },
    onError: () => {
      toast.error(t("RecallError"));
    },
  });

  const timelineLabels: RequestTimelineLabels = {
    empty: t("TimelineEmpty"),
    stepType: {
      DEPUTY: t("DeputyLabel"),
      MANAGER: t("ManagerLabel"),
      RECALL: t("RecallLabel"),
    } satisfies Record<ApprovalStepType, string>,
    stepStatus: {
      PENDING: t("StepStatus.PENDING"),
      APPROVED: t("StepStatus.APPROVED"),
      REJECTED: t("StepStatus.REJECTED"),
      SKIPPED: t("StepStatus.SKIPPED"),
    } satisfies Record<StepStatus, string>,
  };

  if (isLoading) {
    return (
      <div className="mx-auto flex max-w-4xl flex-col gap-6">
        <Skeleton className="h-40 w-full rounded-xl" />
        <Skeleton className="h-64 w-full rounded-xl" />
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div className="mx-auto max-w-4xl">
        <EmptyState title={t("Error")} />
      </div>
    );
  }

  const isOwnRequest = data.applicantId === user?.employeeId;
  const canRecall = data.status === "APPROVED" && isOwnRequest;
  const isPendingRecall = data.status === "PENDING_RECALL";

  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-6">
      <PageHeader
        title={t("Title")}
        description={`${t(`LeaveType.${data.type}`)} · ${formatDuration(data.durationMinutes)}`}
        backHref={backHref}
        backLabel={backLabel}
        actions={
          <div className="flex items-center gap-2">
            <StatusBadge status={data.status} label={t(`StatusLabel.${data.status}`)} />
            {canRecall && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => setShowRecallDialog(true)}
              >
                {t("RecallButton")}
              </Button>
            )}
            {isPendingRecall && isOwnRequest && (
              <span className="text-muted-foreground text-sm">{t("RecallPending")}</span>
            )}
          </div>
        }
      />

      <Section>
        <DescriptionList>
          <DescriptionItem term={t("Fields.Applicant")}>
            {data.applicantName}
          </DescriptionItem>
          <DescriptionItem term={t("Fields.DateRange")}>
            {formatDate(data.startTime)} – {formatDate(data.endTime)}
          </DescriptionItem>
          <DescriptionItem term={t("Fields.Deputy")}>
            {data.deputyName ?? t("NoDeputy")}
          </DescriptionItem>
          <DescriptionItem term={t("Fields.SubmittedAt")}>
            {formatDate(data.createdAt)}
          </DescriptionItem>
          <DescriptionItem term={t("Fields.Reason")} className="sm:col-span-2">
            {data.reason ?? t("NoReason")}
          </DescriptionItem>
        </DescriptionList>
      </Section>

      {canViewAiReview ? <AiReviewSection requestId={requestId} /> : null}

      <Section title={t("TimelineTitle")}>
        <RequestTimeline
          steps={data.approvalSteps}
          actions={data.approvalActions}
          labels={timelineLabels}
        />
      </Section>

      <Dialog
        open={showRecallDialog}
        onOpenChange={(open) => {
          setShowRecallDialog(open);
          if (!open) setRecallReason("");
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t("RecallConfirmTitle")}</DialogTitle>
            <DialogDescription>{t("RecallConfirmDesc")}</DialogDescription>
          </DialogHeader>
          <div className="flex flex-col gap-2">
            <Label htmlFor="recall-reason">{t("RecallReasonLabel")}</Label>
            <Textarea
              id="recall-reason"
              value={recallReason}
              onChange={(e) => setRecallReason(e.target.value)}
              placeholder={t("RecallReasonPlaceholder")}
              rows={3}
            />
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setShowRecallDialog(false)}
              disabled={recallMutation.isPending}
            >
              {t("Cancel")}
            </Button>
            <Button
              variant="destructive"
              onClick={() => recallMutation.mutate({ id: requestId, reason: recallReason })}
              disabled={recallMutation.isPending || recallReason.trim().length === 0}
            >
              {t("RecallButton")}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
