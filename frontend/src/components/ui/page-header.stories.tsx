import type { Meta, StoryObj } from "@storybook/nextjs-vite"

import { Button } from "./button"
import { PageHeader } from "./page-header"

const meta: Meta<typeof PageHeader> = {
  title: "UI/PageHeader",
  component: PageHeader,
  tags: ["autodocs"],
}
export default meta

type Story = StoryObj<typeof PageHeader>

export const Default: Story = {
  args: { title: "My Requests" },
}

export const WithDescription: Story = {
  args: {
    title: "Leave Requests",
    description: "Manage and track your leave applications",
  },
}

export const WithActions: Story = {
  args: {
    title: "My Requests",
    actions: <Button size="sm">New Request</Button>,
  },
}

export const WithBackLink: Story = {
  args: {
    title: "Request #42",
    description: "Annual leave · 3 days",
    backHref: "/requests",
    backLabel: "My Requests",
    actions: <Button size="sm" variant="outline">Export</Button>,
  },
}
