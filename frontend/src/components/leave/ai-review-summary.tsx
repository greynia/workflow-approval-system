import * as React from "react";

import { Badge } from "@/components/ui/badge";
import type {
  AiRecommendation,
  PolicyReference,
  AiReviewResponse,
  RiskLevel,
} from "@/types/leave";

export type AiReviewSummaryLabels = {
  title: string;
  disclaimer: React.ReactNode;
  unavailable: string;
  pending: string;
  failed: string;
  summary: string;
  riskLevelLabel: string;
  riskLevel: Record<RiskLevel, string>;
  hardRuleFlags: string;
  riskReasons: string;
  recommendationLabel: string;
  recommendation: Record<AiRecommendation, string>;
  recommendationReason: string;
  policyReferences: string;
  generatedAt: string;
};

export type AiReviewSummaryProps = {
  review: AiReviewResponse;
  labels: AiReviewSummaryLabels;
  formatDate?: (iso: string) => string;
};

const defaultFormatDate = (iso: string) => new Date(iso).toLocaleString();

const RISK_VARIANT: Record<RiskLevel, React.ComponentProps<typeof Badge>["variant"]> = {
  LOW: "success",
  MEDIUM: "warning",
  HIGH: "destructive",
};

function normalizeRiskReason(reason: string): string {
  return reason.toLowerCase().replace(/[\s\p{P}]+/gu, "");
}

function policyReferenceKey(ref: PolicyReference): string {
  return `${ref.source}-${ref.section}-${ref.chunkIndex}`;
}

function AiReviewSummary({
  review,
  labels,
  formatDate = defaultFormatDate,
}: AiReviewSummaryProps) {
  if (review.status === "PENDING") {
    return <p className="text-sm text-muted-foreground">{labels.pending}</p>;
  }

  if (review.status === "FAILED") {
    return <p className="text-sm text-muted-foreground">{labels.failed}</p>;
  }

  if (review.status !== "COMPLETED") {
    return null;
  }

  const { riskLevel, recommendation } = review;

  const hardRuleReasonKeys = new Set(
    review.hardRuleFlags.map((flag) => normalizeRiskReason(flag.humanReadable)),
  );
  const additionalRiskReasons = review.riskReasons.filter(
    (reason) => !hardRuleReasonKeys.has(normalizeRiskReason(reason)),
  );
  const policyReferences = review.policyReferences ?? [];

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-3">
        <span className="text-sm font-medium text-muted-foreground">
          {labels.riskLevelLabel}
        </span>
        {riskLevel ? (
          <Badge variant={RISK_VARIANT[riskLevel]}>
            {labels.riskLevel[riskLevel]}
          </Badge>
        ) : (
          <Badge variant="secondary">{labels.unavailable}</Badge>
        )}
      </div>

      {review.summary ? (
        <div>
          <p className="text-sm font-medium text-muted-foreground">
            {labels.summary}
          </p>
          <p className="mt-1 text-sm text-foreground">{review.summary}</p>
        </div>
      ) : null}

      {review.hardRuleFlags.length > 0 ? (
        <div>
          <p className="text-sm font-medium text-muted-foreground">
            {labels.hardRuleFlags}
          </p>
          <ul className="mt-1 flex flex-col gap-1">
            {review.hardRuleFlags.map((flag) => (
              <li key={flag.code} className="text-sm text-foreground">
                · {flag.humanReadable}
              </li>
            ))}
          </ul>
        </div>
      ) : null}

      {additionalRiskReasons.length > 0 ? (
        <div>
          <p className="text-sm font-medium text-muted-foreground">
            {labels.riskReasons}
          </p>
          <ul className="mt-1 flex flex-col gap-1">
            {additionalRiskReasons.map((reason, i) => (
              <li key={i} className="text-sm text-foreground">
                · {reason}
              </li>
            ))}
          </ul>
        </div>
      ) : null}

      <div className="flex items-center gap-3">
        <span className="text-sm font-medium text-muted-foreground">
          {labels.recommendationLabel}
        </span>
        <Badge variant="info">
          {recommendation
            ? labels.recommendation[recommendation]
            : labels.unavailable}
        </Badge>
      </div>

      {review.recommendationReason ? (
        <div>
          <p className="text-sm font-medium text-muted-foreground">
            {labels.recommendationReason}
          </p>
          <p className="mt-1 text-sm text-foreground">
            {review.recommendationReason}
          </p>
        </div>
      ) : null}

      {policyReferences.length > 0 ? (
        <div>
          <p className="text-sm font-medium text-muted-foreground">
            {labels.policyReferences}
          </p>
          <ul className="mt-1 flex flex-col gap-2">
            {policyReferences.map((ref) => (
              <li
                key={policyReferenceKey(ref)}
                className="border-l-2 border-info pl-2 text-sm text-foreground"
              >
                <span className="font-medium">{ref.section}</span>
                <p className="mt-0.5 text-xs text-muted-foreground">
                  {ref.content}
                </p>
              </li>
            ))}
          </ul>
        </div>
      ) : null}

      <p className="text-xs text-muted-foreground">
        {labels.generatedAt} {formatDate(review.createdAt)}
        {review.modelName ? ` · ${review.modelName}` : ""}
      </p>
    </div>
  );
}

export { AiReviewSummary, policyReferenceKey };
