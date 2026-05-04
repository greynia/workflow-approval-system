import * as React from "react";

import { StatusBadge } from "@/components/ui/status-badge";
import type {
  ApprovalActionResponse,
  ApprovalStepResponse,
  ApprovalStepType,
  StepStatus,
} from "@/types/leave";

export type RequestTimelineLabels = {
  empty: React.ReactNode;
  stepType: Record<ApprovalStepType, string>;
  stepStatus: Record<StepStatus, string>;
};

export type RequestTimelineProps = {
  steps: ApprovalStepResponse[];
  actions: ApprovalActionResponse[];
  labels: RequestTimelineLabels;
  formatDate?: (iso: string) => string;
};

const defaultFormatDate = (iso: string) => new Date(iso).toLocaleString();

function RequestTimeline({
  steps,
  actions,
  labels,
  formatDate = defaultFormatDate,
}: RequestTimelineProps) {
  if (steps.length === 0) {
    return <p className="text-sm text-muted-foreground">{labels.empty}</p>;
  }

  return (
    <ol className="flex flex-col gap-3">
      {steps.map((step) => {
        const action = actions.find((item) => item.actorId === step.approverId);
        return (
          <li
            key={step.id}
            className="rounded-lg border border-border bg-muted/30 p-4"
          >
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p className="text-sm font-medium text-foreground">
                  {labels.stepType[step.stepType]} · {step.approverName}
                </p>
                <p className="mt-1 text-xs text-muted-foreground">
                  {labels.stepStatus[step.status]} · {formatDate(step.updatedAt)}
                </p>
              </div>
              <StatusBadge
                status={step.status}
                label={labels.stepStatus[step.status]}
              />
            </div>
            {action?.comment ? (
              <p className="mt-3 text-sm text-foreground/80">{action.comment}</p>
            ) : null}
          </li>
        );
      })}
    </ol>
  );
}

export { RequestTimeline };
