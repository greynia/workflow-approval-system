"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useParams } from "next/navigation";
import { Link, usePathname } from "@/i18n/navigation";
import LeaveService from "@/services/leave.service";
import type { ApprovalActionResponse, ApprovalStepResponse } from "@/types/leave";
import { useFormatDurationAsHours } from "@/lib/use-format-duration";
import { useAuth } from "@/hooks/useAuth";
import { useAuthority } from "@/hooks/useAuthority";

const STATUS_STYLES: Record<string, string> = {
  PENDING: "bg-amber-100 text-amber-700",
  APPROVED: "bg-green-100 text-green-700",
  REJECTED: "bg-red-100 text-red-700",
  CANCELLED: "bg-zinc-100 text-zinc-500",
  SKIPPED: "bg-zinc-100 text-zinc-500",
};

function formatDate(date: string): string {
  return new Date(date).toLocaleString();
}

const RISK_STYLES: Record<string, string> = {
  LOW: "bg-green-100 text-green-700",
  MEDIUM: "bg-amber-100 text-amber-700",
  HIGH: "bg-red-100 text-red-700",
};

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

  if (isLoading) {
    return <LoadingState />;
  }

  if (isError || !data) {
    return <p className="py-12 text-center text-sm text-zinc-500">{t("Error")}</p>;
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <Link
        href={backHref}
        className="inline-flex items-center gap-1.5 text-sm font-medium text-zinc-500 transition-colors hover:text-zinc-900"
      >
        <span aria-hidden="true">‹</span>
        {backLabel}
      </Link>

      <section className="rounded-lg border border-zinc-200 bg-white p-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="text-xl font-semibold text-zinc-900">{t("Title")}</h1>
            <p className="mt-1 text-sm text-zinc-500">
              {t(`LeaveType.${data.type}`)} · {formatDuration(data.durationMinutes)}
            </p>
          </div>
          <span className={`inline-flex rounded-full px-3 py-1 text-xs font-medium ${STATUS_STYLES[data.status]}`}>
            {t(`StatusLabel.${data.status}`)}
          </span>
        </div>

        <dl className="mt-6 grid gap-4 sm:grid-cols-2">
          <DetailItem label={t("Fields.Applicant")} value={data.applicantName} />
          <DetailItem label={t("Fields.DateRange")} value={`${formatDate(data.startTime)} - ${formatDate(data.endTime)}`} />
          <DetailItem label={t("Fields.Deputy")} value={data.deputyName ?? t("NoDeputy")} />
          <DetailItem label={t("Fields.SubmittedAt")} value={formatDate(data.createdAt)} />
          <DetailItem label={t("Fields.Reason")} value={data.reason ?? t("NoReason")} fullWidth />
        </dl>
      </section>

      {canViewAiReview && (
        <AiReviewSection requestId={requestId} />
      )}

      <section className="rounded-lg border border-zinc-200 bg-white p-6">
        <h2 className="text-lg font-semibold text-zinc-900">{t("TimelineTitle")}</h2>
        <Timeline steps={data.approvalSteps} actions={data.approvalActions} />
      </section>
    </div>
  );
}

function AiReviewSection({ requestId }: { requestId: number }) {
  const t = useTranslations("Requests.Detail.AiReview");
  const { data, isLoading } = useQuery({
    queryKey: ["requests", requestId, "ai-review"],
    queryFn: () => LeaveService.getAiReview(requestId),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (!status) return false;
      return status === "PENDING" ? 5000 : false;
    },
  });

  if (isLoading) {
    return <div className="h-24 animate-pulse rounded-lg bg-zinc-100" />;
  }

  if (!data) return null;

  const riskLevel = data.riskLevel;
  const recommendation = data.recommendation;
  const hardRuleReasonKeys = new Set(
    data.hardRuleFlags.map((flag) => normalizeRiskReason(flag.humanReadable))
  );
  const additionalRiskReasons = data.riskReasons.filter(
    (reason) => !hardRuleReasonKeys.has(normalizeRiskReason(reason))
  );

  return (
    <section className="rounded-lg border border-zinc-200 bg-white p-6">
      <div className="flex items-center gap-2">
        <h2 className="text-lg font-semibold text-zinc-900">{t("Title")}</h2>
        <span className="text-sm text-zinc-400">{t("Disclaimer")}</span>
      </div>

      {data.status === "PENDING" && (
        <p className="mt-4 text-sm text-zinc-500">{t("Pending")}</p>
      )}

      {data.status === "FAILED" && (
        <p className="mt-4 text-sm text-zinc-400">{t("Failed")}</p>
      )}

      {data.status === "COMPLETED" && (
        <div className="mt-4 space-y-4">
          <div className="flex items-center gap-3">
            <span className="text-sm font-medium text-zinc-600">{t("RiskLevel.Label")}</span>
            <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium ${riskLevel ? RISK_STYLES[riskLevel] : "bg-zinc-100 text-zinc-500"}`}>
              {riskLevel ? t(`RiskLevel.${riskLevel}`) : t("Unavailable")}
            </span>
          </div>

          {data.summary && (
            <div>
              <p className="text-sm font-medium text-zinc-600">{t("Summary")}</p>
              <p className="mt-1 text-sm text-zinc-800">{data.summary}</p>
            </div>
          )}

          {data.hardRuleFlags.length > 0 && (
            <div>
              <p className="text-sm font-medium text-zinc-600">{t("HardRuleFlags")}</p>
              <ul className="mt-1 space-y-1">
                {data.hardRuleFlags.map((flag) => (
                  <li key={flag.code} className="text-sm text-zinc-700">· {flag.humanReadable}</li>
                ))}
              </ul>
            </div>
          )}

          {additionalRiskReasons.length > 0 && (
            <div>
              <p className="text-sm font-medium text-zinc-600">{t("RiskReasons")}</p>
              <ul className="mt-1 space-y-1">
                {additionalRiskReasons.map((reason, i) => (
                  <li key={i} className="text-sm text-zinc-700">· {reason}</li>
                ))}
              </ul>
            </div>
          )}

          <div className="flex items-center gap-3">
            <span className="text-sm font-medium text-zinc-600">{t("Recommendation.Label")}</span>
            <span className="inline-flex rounded-full bg-blue-50 px-2.5 py-0.5 text-xs font-medium text-blue-700">
              {recommendation ? t(`Recommendation.${recommendation}`) : t("Unavailable")}
            </span>
          </div>

          {data.recommendationReason && (
            <div>
              <p className="text-sm font-medium text-zinc-600">{t("RecommendationReason")}</p>
              <p className="mt-1 text-sm text-zinc-700">{data.recommendationReason}</p>
            </div>
          )}

          <p className="text-xs text-zinc-400">
            {t("GeneratedAt")} {formatDate(data.createdAt)}
            {data.modelName ? ` · ${data.modelName}` : ""}
          </p>
        </div>
      )}
    </section>
  );
}

function normalizeRiskReason(reason: string): string {
  return reason.toLowerCase().replace(/[\s\p{P}]+/gu, "");
}

function LoadingState() {
  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div className="h-40 animate-pulse rounded-lg bg-zinc-100" />
      <div className="h-64 animate-pulse rounded-lg bg-zinc-100" />
    </div>
  );
}

function DetailItem({
  label,
  value,
  fullWidth = false,
}: {
  label: string;
  value: string;
  fullWidth?: boolean;
}) {
  return (
    <div className={fullWidth ? "sm:col-span-2" : ""}>
      <dt className="text-sm font-medium text-zinc-500">{label}</dt>
      <dd className="mt-1 text-sm text-zinc-900">{value}</dd>
    </div>
  );
}

function Timeline({
  steps,
  actions,
}: {
  steps: ApprovalStepResponse[];
  actions: ApprovalActionResponse[];
}) {
  const t = useTranslations("Requests.Detail");

  if (steps.length === 0) {
    return <p className="mt-4 text-sm text-zinc-500">{t("TimelineEmpty")}</p>;
  }

  return (
    <ol className="mt-6 space-y-4">
      {steps.map((step) => {
        const action = actions.find((item) => item.actorId === step.approverId);

        return (
          <li key={step.id} className="relative rounded-lg border border-zinc-200 bg-zinc-50 p-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p className="text-sm font-medium text-zinc-900">
                  {step.stepType === "DEPUTY" ? t("DeputyLabel") : t("ManagerLabel")} · {step.approverName}
                </p>
                <p className="mt-1 text-xs text-zinc-500">
                  {t(`StepStatus.${step.status}`)} · {formatDate(step.updatedAt)}
                </p>
              </div>
              <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_STYLES[step.status]}`}>
                {t(`StepStatus.${step.status}`)}
              </span>
            </div>
            {action?.comment ? <p className="mt-3 text-sm text-zinc-600">{action.comment}</p> : null}
          </li>
        );
      })}
    </ol>
  );
}
