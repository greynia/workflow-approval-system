import type { Meta, StoryObj } from "@storybook/nextjs-vite"

import { Button } from "./button"
import { Section } from "./section"

const meta: Meta<typeof Section> = {
  title: "UI/Section",
  component: Section,
  tags: ["autodocs"],
}
export default meta

type Story = StoryObj<typeof Section>

export const Default: Story = {
  args: {
    title: "Request Details",
    children: <p className="text-sm text-muted-foreground">Content goes here.</p>,
  },
}

export const WithActions: Story = {
  args: {
    title: "AI Review",
    description: "Automated analysis of this request",
    actions: <Button size="sm" variant="outline">Retry</Button>,
    children: <p className="text-sm">Analysis complete. No issues found.</p>,
  },
}

export const NoHeader: Story = {
  args: {
    children: <p className="text-sm text-muted-foreground">A section without a header.</p>,
  },
}
