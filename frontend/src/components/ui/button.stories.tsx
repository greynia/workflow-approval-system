import type { Meta, StoryObj } from "@storybook/nextjs-vite"
import { Plus, Trash2 } from "lucide-react"

import { Button } from "./button"

const meta: Meta<typeof Button> = {
  title: "UI/Button",
  component: Button,
  tags: ["autodocs"],
  argTypes: {
    variant: {
      control: "select",
      options: ["default", "secondary", "outline", "ghost", "destructive", "success", "link"],
    },
    size: {
      control: "select",
      options: ["default", "sm", "lg", "xs", "icon", "icon-sm", "icon-lg"],
    },
  },
}
export default meta
type Story = StoryObj<typeof Button>

const VARIANTS = ["default", "secondary", "outline", "ghost", "destructive", "success"] as const

export const Default: Story = { args: { children: "Button" } }

export const WithIcon: Story = {
  args: { children: (<><Plus className="size-4" />New Request</>) as unknown as string },
}

export const IconOnly: Story = {
  args: { size: "icon", variant: "ghost", children: (<Trash2 className="size-4" />) as unknown as string },
}

export const AllVariants: Story = {
  name: "All Variants",
  render: () => (
    <div className="space-y-6">
      <div>
        <p className="mb-3 text-xs text-muted-foreground">Interactive — hover or tab to see states</p>
        <div className="flex flex-wrap gap-3">
          {VARIANTS.map((v) => (
            <Button key={v} variant={v}>
              {v.charAt(0).toUpperCase() + v.slice(1)}
            </Button>
          ))}
        </div>
      </div>
      <div>
        <p className="mb-3 text-xs text-muted-foreground">Disabled</p>
        <div className="flex flex-wrap gap-3">
          {VARIANTS.map((v) => (
            <Button key={v} variant={v} disabled>
              {v.charAt(0).toUpperCase() + v.slice(1)}
            </Button>
          ))}
        </div>
      </div>
    </div>
  ),
}
