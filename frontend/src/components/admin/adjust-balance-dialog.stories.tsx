import { useState } from "react";
import type { Meta, StoryObj } from "@storybook/nextjs-vite";

import { Button } from "@/components/ui/button";
import { AdjustBalanceDialog } from "./adjust-balance-dialog";
import type { AdminLeaveBalance } from "@/types/leave";

const meta: Meta<typeof AdjustBalanceDialog> = {
  title: "Admin/AdjustBalanceDialog",
  component: AdjustBalanceDialog,
  parameters: { layout: "fullscreen" },
};
export default meta;

type Story = StoryObj<typeof AdjustBalanceDialog>;

const BALANCE: AdminLeaveBalance = {
  id: 42,
  employeeId: 7,
  employeeName: "王小明",
  leaveType: "ANNUAL",
  quotaMinutes: 6720,
  usedMinutes: 1440,
  remainingMinutes: 5280,
  updatedAt: "2026-01-01T00:00:00Z",
};

export const Open: Story = {
  render: () => {
    const [open, setOpen] = useState(true);
    return (
      <div className="p-6">
        <Button onClick={() => setOpen(true)}>Re-open dialog</Button>
        <AdjustBalanceDialog
          open={open}
          balance={BALANCE}
          onClose={() => setOpen(false)}
          onSuccess={() => setOpen(false)}
        />
      </div>
    );
  },
};

export const SickLeave: Story = {
  render: () => {
    const [open, setOpen] = useState(true);
    return (
      <div className="p-6">
        <Button onClick={() => setOpen(true)}>Re-open dialog</Button>
        <AdjustBalanceDialog
          open={open}
          balance={{ ...BALANCE, leaveType: "SICK", quotaMinutes: 2400, usedMinutes: 480, remainingMinutes: 1920 }}
          onClose={() => setOpen(false)}
          onSuccess={() => setOpen(false)}
        />
      </div>
    );
  },
};

export const Closed: Story = {
  render: () => (
    <AdjustBalanceDialog
      open={false}
      balance={null}
      onClose={() => {}}
      onSuccess={() => {}}
    />
  ),
};
