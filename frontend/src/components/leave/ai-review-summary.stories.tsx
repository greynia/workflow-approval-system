import type { Meta, StoryObj } from "@storybook/nextjs-vite";

import type { AiReviewResponse } from "@/types/leave";
import {
  AiReviewSummary,
  type AiReviewSummaryLabels,
} from "./ai-review-summary";

const LABELS: AiReviewSummaryLabels = {
  title: "AI Review Summary",
  disclaimer: "(For reference only)",
  unavailable: "Unavailable",
  pending: "AI analysis in progress, please wait...",
  failed: "AI analysis temporarily unavailable",
  summary: "Summary",
  riskLevelLabel: "Risk Level",
  riskLevel: {
    LOW: "Low Risk",
    MEDIUM: "Medium Risk",
    HIGH: "High Risk",
  },
  hardRuleFlags: "Rules Triggered",
  riskReasons: "Additional Notes",
  recommendationLabel: "Recommendation",
  recommendation: {
    APPROVE: "Recommended: Approve",
    REVIEW_CAREFULLY: "Recommended: Review Carefully",
    ESCALATE: "Recommended: Escalate",
    INSUFFICIENT_INFORMATION: "Insufficient information",
  },
  recommendationReason: "Recommendation Reason",
  generatedAt: "Generated At",
};

const BASE: AiReviewResponse = {
  status: "COMPLETED",
  summary: "Routine 2-day annual leave. No conflicts found.",
  riskLevel: "LOW",
  riskReasons: [],
  hardRuleFlags: [],
  recommendation: "APPROVE",
  recommendationReason: "All rules pass; deputy is available.",
  modelName: "gemini-1.5-flash",
  promptVersion: "v3",
  provider: "GEMINI",
  errorCode: null,
  createdAt: "2026-05-01T09:30:00Z",
};

const meta: Meta<typeof AiReviewSummary> = {
  title: "Leave/AiReviewSummary",
  component: AiReviewSummary,
};
export default meta;
type Story = StoryObj<typeof AiReviewSummary>;

const Wrap = ({ children }: { children: React.ReactNode }) => (
  <div className="max-w-2xl rounded-xl bg-card p-5 ring-1 ring-foreground/10">
    {children}
  </div>
);

export const Pending: Story = {
  render: () => (
    <Wrap>
      <AiReviewSummary
        review={{ ...BASE, status: "PENDING", summary: null, riskLevel: null, recommendation: null, recommendationReason: null }}
        labels={LABELS}
      />
    </Wrap>
  ),
};

export const Failed: Story = {
  render: () => (
    <Wrap>
      <AiReviewSummary
        review={{ ...BASE, status: "FAILED", errorCode: "TIMEOUT" }}
        labels={LABELS}
      />
    </Wrap>
  ),
};

export const LowRisk: Story = {
  render: () => (
    <Wrap>
      <AiReviewSummary review={BASE} labels={LABELS} />
    </Wrap>
  ),
};

export const MediumRisk: Story = {
  render: () => (
    <Wrap>
      <AiReviewSummary
        review={{
          ...BASE,
          riskLevel: "MEDIUM",
          recommendation: "REVIEW_CAREFULLY",
          summary: "Late submission and partial deputy availability.",
          riskReasons: [
            "Submitted only 1 day before the leave start.",
            "Deputy already assigned to another concurrent request.",
          ],
        }}
        labels={LABELS}
      />
    </Wrap>
  ),
};

export const HighRisk: Story = {
  render: () => (
    <Wrap>
      <AiReviewSummary
        review={{
          ...BASE,
          riskLevel: "HIGH",
          recommendation: "ESCALATE",
          summary: "Multiple rule violations detected.",
          riskReasons: ["Quota would be exceeded by this request."],
          hardRuleFlags: [
            { code: "QUOTA_EXCEEDED", level: "HIGH", humanReadable: "Annual leave quota exceeded by 4 hours." },
            { code: "BLACKOUT_PERIOD", level: "HIGH", humanReadable: "Falls inside the year-end freeze window." },
          ],
          recommendationReason: "Quota and blackout period both violated.",
        }}
        labels={LABELS}
      />
    </Wrap>
  ),
};
