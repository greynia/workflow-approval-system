import type { Meta, StoryObj } from "@storybook/nextjs-vite"

import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "./card"
import { Button } from "./button"

const meta: Meta<typeof Card> = {
  title: "UI/Card",
  component: Card,
  tags: ["autodocs"],
}
export default meta

type Story = StoryObj<typeof Card>

export const Default: Story = {
  render: () => (
    <Card className="w-80">
      <CardHeader>
        <CardTitle>Annual Leave Request</CardTitle>
        <CardDescription>Submitted on 2026-01-15</CardDescription>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground">Duration: 3 days (Jan 20–22)</p>
      </CardContent>
      <CardFooter>
        <Button size="sm" variant="outline">View Details</Button>
      </CardFooter>
    </Card>
  ),
}

export const Small: Story = {
  render: () => (
    <Card size="sm" className="w-72">
      <CardHeader>
        <CardTitle>Balance Summary</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-2xl font-bold">12</p>
        <p className="text-xs text-muted-foreground">days remaining</p>
      </CardContent>
    </Card>
  ),
}
