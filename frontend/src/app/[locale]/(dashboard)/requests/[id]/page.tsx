"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useParams } from "next/navigation";
import { usePathname } from "@/i18n/navigation";
import LeaveService from "@/services/leave.service";
import { useFormatDurationAsHours } from "@/lib/use-format-duration";
import { useAuth } from "@/hooks/useAuth";
import { useAuthority } from "@/hooks/useAuthority";
import { PageHeader } from "@/components/ui/page-header";
import { Section } from "@/components/ui/section";
import {
  DescriptionList,
  DescriptionItem,
} from "@/components/ui/description-list";
import { StatusBadge } from "@/components/ui/status-badge";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/ui/empty-state";
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
  const backHref = isApprovalContext ? "/approvals" : "/requests";
  const backLabel = isApprovalContext ? t("BackToApprovals") : t("BackToRequests");
  const { user } = useAuth();
  const canViewAiReview = useAuthority(user?.permissions ?? [], ["approval.view"]);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["requests", requestId],
    queryFn: () => LeaveService.getDetail(requestId),
    enabled: Number.isInteger(requestId) && requestId > 0,
  });

  const timelineLabels: RequestTimelineLabels = {
    empty: t("TimelineEmpty"),
    stepType: {
      DEPUTY: t("DeputyLabel"),
      MANAGER: t("ManagerLabel"),
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

  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-6">
      <PageHeader
        title={t("Title")}
        description={`${t(`LeaveType.${data.type}`)} · ${formatDuration(data.durationMinutes)}`}
        backHref={backHref}
        backLabel={backLabel}
        actions={
          <StatusBadge status={data.status} label={t(`StatusLabel.${data.status}`)} />
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
    </div>
  );
}
