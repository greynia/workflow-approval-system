import type { Meta, StoryObj } from "@storybook/nextjs-vite"

import { StatusBadge } from "./status-badge"

const meta: Meta<typeof StatusBadge> = {
  title: "UI/StatusBadge",
  component: StatusBadge,
  tags: ["autodocs"],
}
export default meta

type Story = StoryObj<typeof StatusBadge>

export const Pending: Story = { args: { status: "PENDING", label: "Pending" } }
export const Approved: Story = { args: { status: "APPROVED", label: "Approved" } }
export const Rejected: Story = { args: { status: "REJECTED", label: "Rejected" } }
export const Cancelled: Story = { args: { status: "CANCELLED", label: "Cancelled" } }
export const Skipped: Story = { args: { status: "SKIPPED", label: "Skipped" } }

export const AllStatuses: Story = {
  render: () => (
    <div className="flex flex-wrap gap-2">
      {(["PENDING", "APPROVED", "REJECTED", "CANCELLED", "SKIPPED"] as const).map((s) => (
        <StatusBadge key={s} status={s} label={s} />
      ))}
    </div>
  ),
}
