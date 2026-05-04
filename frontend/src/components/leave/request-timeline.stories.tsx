import type { Meta, StoryObj } from "@storybook/nextjs-vite";

import type {
  ApprovalActionResponse,
  ApprovalStepResponse,
} from "@/types/leave";
import { RequestTimeline, type RequestTimelineLabels } from "./request-timeline";

const LABELS: RequestTimelineLabels = {
  empty: "No approval steps yet",
  stepType: {
    DEPUTY: "Deputy",
    MANAGER: "Manager",
  },
  stepStatus: {
    PENDING: "Pending",
    APPROVED: "Approved",
    REJECTED: "Rejected",
    SKIPPED: "Skipped",
  },
};

const DEPUTY_PENDING: ApprovalStepResponse = {
  id: 1,
  approverId: 11,
  approverName: "Bob Wang",
  stepType: "DEPUTY",
  status: "PENDING",
  createdAt: "2026-05-01T09:00:00Z",
  updatedAt: "2026-05-01T09:00:00Z",
};
const DEPUTY_APPROVED: ApprovalStepResponse = {
  ...DEPUTY_PENDING,
  status: "APPROVED",
  updatedAt: "2026-05-01T10:30:00Z",
};
const MANAGER_PENDING: ApprovalStepResponse = {
  id: 2,
  approverId: 21,
  approverName: "Carol Lin",
  stepType: "MANAGER",
  status: "PENDING",
  createdAt: "2026-05-01T10:30:00Z",
  updatedAt: "2026-05-01T10:30:00Z",
};
const MANAGER_REJECTED: ApprovalStepResponse = {
  ...MANAGER_PENDING,
  status: "REJECTED",
  updatedAt: "2026-05-01T11:15:00Z",
};
const APPROVE_ACTION: ApprovalActionResponse = {
  id: 100,
  actorId: 11,
  actorName: "Bob Wang",
  actionType: "APPROVE",
  comment: null,
  createdAt: "2026-05-01T10:30:00Z",
};
const REJECT_ACTION: ApprovalActionResponse = {
  id: 101,
  actorId: 21,
  actorName: "Carol Lin",
  actionType: "REJECT",
  comment: "Workload conflict during the requested period.",
  createdAt: "2026-05-01T11:15:00Z",
};

const meta: Meta<typeof RequestTimeline> = {
  title: "Leave/RequestTimeline",
  component: RequestTimeline,
};
export default meta;
type Story = StoryObj<typeof RequestTimeline>;

export const Empty: Story = {
  render: () => (
    <RequestTimeline steps={[]} actions={[]} labels={LABELS} />
  ),
};

export const InProgress: Story = {
  render: () => (
    <div className="max-w-2xl">
      <RequestTimeline
        steps={[DEPUTY_APPROVED, MANAGER_PENDING]}
        actions={[APPROVE_ACTION]}
        labels={LABELS}
      />
    </div>
  ),
};

export const Approved: Story = {
  render: () => (
    <div className="max-w-2xl">
      <RequestTimeline
        steps={[DEPUTY_APPROVED, { ...MANAGER_PENDING, status: "APPROVED", updatedAt: "2026-05-01T11:00:00Z" }]}
        actions={[APPROVE_ACTION]}
        labels={LABELS}
      />
    </div>
  ),
};

export const Rejected: Story = {
  render: () => (
    <div className="max-w-2xl">
      <RequestTimeline
        steps={[DEPUTY_APPROVED, MANAGER_REJECTED]}
        actions={[APPROVE_ACTION, REJECT_ACTION]}
        labels={LABELS}
      />
    </div>
  ),
};
