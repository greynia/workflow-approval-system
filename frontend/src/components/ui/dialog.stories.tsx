import { useState } from "react"
import type { Meta, StoryObj } from "@storybook/nextjs-vite"
import { AlertTriangle, CheckCircle } from "lucide-react"

import { Button } from "./button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "./dialog"

const meta: Meta = {
  title: "UI/Dialog",
  parameters: { layout: "centered" },
}
export default meta
type Story = StoryObj

export const Default: Story = {
  render: () => (
    <Dialog>
      <DialogTrigger asChild>
        <Button>Open Dialog</Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Confirm Action</DialogTitle>
          <DialogDescription>
            This action cannot be undone. Are you sure you want to continue?
          </DialogDescription>
        </DialogHeader>
        <DialogFooter showCloseButton>
          <Button>Confirm</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  ),
}

export const ApprovalDialog: Story = {
  name: "Approval Action",
  render: () => {
    const [result, setResult] = useState<string | null>(null)
    return (
      <div className="flex flex-col items-center gap-4">
        <Dialog onOpenChange={() => setResult(null)}>
          <DialogTrigger asChild>
            <Button variant="success">Approve Request</Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2">
                <CheckCircle className="size-4 text-success" />
                Approve Leave Request
              </DialogTitle>
              <DialogDescription>
                You are about to approve the leave request from <strong>Alice Chen</strong> for
                3 days (2026-05-05 – 2026-05-07). This will notify the applicant.
              </DialogDescription>
            </DialogHeader>
            <DialogFooter>
              <Button variant="success" onClick={() => setResult("Approved")}>
                Approve
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
        {result && (
          <p className="text-sm text-success">✓ Request {result}</p>
        )}
      </div>
    )
  },
}

export const DestructiveDialog: Story = {
  name: "Destructive Action",
  render: () => {
    const [result, setResult] = useState<string | null>(null)
    return (
      <div className="flex flex-col items-center gap-4">
        <Dialog onOpenChange={() => setResult(null)}>
          <DialogTrigger asChild>
            <Button variant="destructive">Reject Request</Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2">
                <AlertTriangle className="size-4 text-destructive" />
                Reject Leave Request
              </DialogTitle>
              <DialogDescription>
                Rejecting this request will notify <strong>Alice Chen</strong> and the decision
                cannot be reversed.
              </DialogDescription>
            </DialogHeader>
            <DialogFooter>
              <Button variant="destructive" onClick={() => setResult("Rejected")}>
                Reject
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
        {result && (
          <p className="text-sm text-destructive">✗ Request {result}</p>
        )}
      </div>
    )
  },
}

export const ControlledOpen: Story = {
  name: "Controlled (always open)",
  render: () => (
    <Dialog open>
      <DialogContent showCloseButton={false}>
        <DialogHeader>
          <DialogTitle>Always Visible</DialogTitle>
          <DialogDescription>
            This dialog is controlled — useful for displaying loading states or forced confirmations.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter showCloseButton>
          <Button>OK</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  ),
}
