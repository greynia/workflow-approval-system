"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import LeaveService from "@/services/leave.service";
import { Section } from "@/components/ui/section";
import { Skeleton } from "@/components/ui/skeleton";
import {
  AiReviewSummary,
  type AiReviewSummaryLabels,
} from "@/components/leave/ai-review-summary";
import type { AiRecommendation, RiskLevel } from "@/types/leave";

interface AiReviewSectionProps {
  requestId: number;
}

export function AiReviewSection({ requestId }: AiReviewSectionProps) {
  const tAi = useTranslations("Requests.Detail.AiReview");

  const { data, isLoading } = useQuery({
    queryKey: ["requests", requestId, "ai-review"],
    queryFn: () => LeaveService.getAiReview(requestId),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (!status) return false;
      return status === "PENDING" ? 5000 : false;
    },
  });

  const labels: AiReviewSummaryLabels = {
    title: tAi("Title"),
    disclaimer: tAi("Disclaimer"),
    unavailable: tAi("Unavailable"),
    pending: tAi("Pending"),
    failed: tAi("Failed"),
    summary: tAi("Summary"),
    riskLevelLabel: tAi("RiskLevel.Label"),
    riskLevel: {
      LOW: tAi("RiskLevel.LOW"),
      MEDIUM: tAi("RiskLevel.MEDIUM"),
      HIGH: tAi("RiskLevel.HIGH"),
    } satisfies Record<RiskLevel, string>,
    hardRuleFlags: tAi("HardRuleFlags"),
    riskReasons: tAi("RiskReasons"),
    recommendationLabel: tAi("Recommendation.Label"),
    recommendation: {
      APPROVE: tAi("Recommendation.APPROVE"),
      REVIEW_CAREFULLY: tAi("Recommendation.REVIEW_CAREFULLY"),
      ESCALATE: tAi("Recommendation.ESCALATE"),
      INSUFFICIENT_INFORMATION: tAi("Recommendation.INSUFFICIENT_INFORMATION"),
    } satisfies Record<AiRecommendation, string>,
    recommendationReason: tAi("RecommendationReason"),
    policyReferences: tAi("PolicyReferences"),
    generatedAt: tAi("GeneratedAt"),
  };

  if (isLoading) {
    return <Skeleton className="h-24 w-full rounded-xl" />;
  }

  if (!data) return null;

  return (
    <Section title={labels.title} description={labels.disclaimer}>
      <AiReviewSummary review={data} labels={labels} />
    </Section>
  );
}
