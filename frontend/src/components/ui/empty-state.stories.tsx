import type { Meta, StoryObj } from "@storybook/nextjs-vite"
import { FileX, Search } from "lucide-react"

import { Button } from "./button"
import { EmptyState } from "./empty-state"

const meta: Meta<typeof EmptyState> = {
  title: "UI/EmptyState",
  component: EmptyState,
  tags: ["autodocs"],
}
export default meta

type Story = StoryObj<typeof EmptyState>

export const Default: Story = {
  args: { title: "No results found" },
}

export const WithDescription: Story = {
  args: {
    icon: <FileX />,
    title: "No requests yet",
    description: "You haven't submitted any leave requests. Start by creating a new one.",
  },
}

export const WithAction: Story = {
  args: {
    icon: <Search />,
    title: "No matching records",
    description: "Try adjusting your filters or search terms.",
    action: <Button size="sm" variant="outline">Clear filters</Button>,
  },
}
