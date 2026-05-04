import { useState } from "react";
import type { Meta, StoryObj } from "@storybook/nextjs-vite";

import { Button } from "@/components/ui/button";
import { ApprovalDialog } from "./approval-dialog";

const meta: Meta<typeof ApprovalDialog> = {
  title: "Approval/ApprovalDialog",
  component: ApprovalDialog,
  parameters: { layout: "centered" },
};
export default meta;

type Story = StoryObj<typeof ApprovalDialog>;

const SUMMARY = "Alice Chen — 2026-05-05 09:00 → 2026-05-07 18:00";

export const ApproveIdle: Story = {
  render: () => {
    const [open, setOpen] = useState(true);
    return (
      <div className="flex flex-col gap-3">
        <Button onClick={() => setOpen(true)}>Re-open dialog</Button>
        <ApprovalDialog
          variant="approve"
          open={open}
          onOpenChange={setOpen}
          title="Confirm Approval"
          description="Are you sure you want to approve this leave request?"
          applicantSummary={SUMMARY}
          confirmLabel="Approve"
          cancelLabel="Cancel"
          onConfirm={() => setOpen(false)}
        />
      </div>
    );
  },
};

export const ApproveSubmitting: Story = {
  render: () => (
    <ApprovalDialog
      variant="approve"
      open
      onOpenChange={() => {}}
      title="Confirm Approval"
      description="Submitting the approval…"
      applicantSummary={SUMMARY}
      confirmLabel="Approve"
      cancelLabel="Cancel"
      isPending
      onConfirm={() => {}}
    />
  ),
};

export const RejectIdle: Story = {
  render: () => {
    const [open, setOpen] = useState(true);
    return (
      <div className="flex flex-col gap-3">
        <Button variant="destructive" onClick={() => setOpen(true)}>
          Re-open dialog
        </Button>
        <ApprovalDialog
          variant="reject"
          open={open}
          onOpenChange={setOpen}
          title="Reject Request"
          description="Please provide a reason. The applicant will see this comment."
          applicantSummary={SUMMARY}
          commentLabel="Reason"
          commentPlaceholder="Enter rejection reason (required)"
          commentRequiredError="Please enter a rejection reason"
          confirmLabel="Reject"
          cancelLabel="Cancel"
          onConfirm={() => setOpen(false)}
        />
      </div>
    );
  },
};

export const RejectSubmitting: Story = {
  render: () => (
    <ApprovalDialog
      variant="reject"
      open
      onOpenChange={() => {}}
      title="Reject Request"
      description="Sending rejection…"
      applicantSummary={SUMMARY}
      commentLabel="Reason"
      commentPlaceholder="Enter rejection reason (required)"
      confirmLabel="Reject"
      cancelLabel="Cancel"
      isPending
      onConfirm={() => {}}
    />
  ),
};

export const RejectError: Story = {
  render: () => (
    <ApprovalDialog
      variant="reject"
      open
      onOpenChange={() => {}}
      title="Reject Request"
      description="Please provide a reason. The applicant will see this comment."
      applicantSummary={SUMMARY}
      commentLabel="Reason"
      commentPlaceholder="Enter rejection reason (required)"
      confirmLabel="Reject"
      cancelLabel="Cancel"
      errorMessage="Action failed. Please try again."
      onConfirm={() => {}}
    />
  ),
};
