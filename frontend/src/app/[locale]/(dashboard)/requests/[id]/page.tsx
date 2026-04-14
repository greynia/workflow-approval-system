"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useParams } from "next/navigation";
import LeaveService from "@/services/leave.service";
import type {
  ApprovalActionResponse,
  ApprovalStepResponse,
} from "@/types/leave";

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

export default function RequestDetailPage() {
  const params = useParams<{ id: string }>();
  const t = useTranslations("Requests.Detail");
  const requestId = Number(params.id);

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
      <section className="rounded-lg border border-zinc-200 bg-white p-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="text-xl font-semibold text-zinc-900">{t("Title")}</h1>
            <p className="mt-1 text-sm text-zinc-500">
              {t(`LeaveType.${data.type}`)} · {data.days} {t("Days")}
            </p>
          </div>
          <span
            className={`inline-flex rounded-full px-3 py-1 text-xs font-medium ${STATUS_STYLES[data.status]}`}
          >
            {t(`StatusLabel.${data.status}`)}
          </span>
        </div>

        <dl className="mt-6 grid gap-4 sm:grid-cols-2">
          <DetailItem label={t("Fields.Applicant")} value={data.applicantName} />
          <DetailItem
            label={t("Fields.DateRange")}
            value={`${data.startDate} - ${data.endDate}`}
          />
          <DetailItem
            label={t("Fields.Deputy")}
            value={data.deputyName ?? t("NoDeputy")}
          />
          <DetailItem
            label={t("Fields.SubmittedAt")}
            value={formatDate(data.createdAt)}
          />
          <DetailItem
            label={t("Fields.Reason")}
            value={data.reason ?? t("NoReason")}
            fullWidth
          />
        </dl>
      </section>

      <section className="rounded-lg border border-zinc-200 bg-white p-6">
        <h2 className="text-lg font-semibold text-zinc-900">{t("TimelineTitle")}</h2>
        <Timeline steps={data.approvalSteps} actions={data.approvalActions} />
      </section>
    </div>
  );
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
          <li
            key={step.id}
            className="relative rounded-lg border border-zinc-200 bg-zinc-50 p-4"
          >
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p className="text-sm font-medium text-zinc-900">
                  {t("StepLabel", { step: step.stepOrder })} · {step.approverName}
                </p>
                <p className="mt-1 text-xs text-zinc-500">
                  {t(`StepStatus.${step.status}`)} · {formatDate(step.updatedAt)}
                </p>
              </div>
              <span
                className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_STYLES[step.status]}`}
              >
                {t(`StepStatus.${step.status}`)}
              </span>
            </div>
            {action?.comment ? (
              <p className="mt-3 text-sm text-zinc-600">{action.comment}</p>
            ) : null}
          </li>
        );
      })}
    </ol>
  );
}
