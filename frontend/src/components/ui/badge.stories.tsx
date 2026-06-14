import type { Meta, StoryObj } from "@storybook/nextjs-vite"

import { Badge } from "./badge"

const meta: Meta<typeof Badge> = {
  title: "UI/Badge",
  component: Badge,
  tags: ["autodocs"],
  argTypes: {
    variant: {
      control: "select",
      options: ["default", "secondary", "destructive", "success", "warning", "info", "outline", "ghost"],
    },
  },
}
export default meta

type Story = StoryObj<typeof Badge>

export const Default: Story = { args: { children: "Default" } }
export const Success: Story = { args: { variant: "success", children: "Approved" } }
export const Warning: Story = { args: { variant: "warning", children: "Pending" } }
export const Destructive: Story = { args: { variant: "destructive", children: "Rejected" } }
export const Info: Story = { args: { variant: "info", children: "Info" } }
export const Secondary: Story = { args: { variant: "secondary", children: "Cancelled" } }
export const Outline: Story = { args: { variant: "outline", children: "Outline" } }

export const AllVariants: Story = {
  render: () => (
    <div className="flex flex-wrap gap-2">
      {(["default", "secondary", "success", "warning", "destructive", "info", "outline"] as const).map(
        (v) => <Badge key={v} variant={v}>{v}</Badge>
      )}
    </div>
  ),
}
