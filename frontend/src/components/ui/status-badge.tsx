import * as React from "react"

import { Badge } from "@/components/ui/badge"
import type { RequestStatus, StepStatus } from "@/types/leave"

type StatusKind = RequestStatus | StepStatus

type BadgeVariant = React.ComponentProps<typeof Badge>["variant"]

const STATUS_VARIANT: Record<StatusKind, BadgeVariant> = {
  PENDING: "warning",
  APPROVED: "success",
  REJECTED: "destructive",
  CANCELLED: "secondary",
  SKIPPED: "outline",
  PENDING_RECALL: "info",
}

function StatusBadge({
  status,
  label,
  className,
  ...props
}: Omit<React.ComponentProps<typeof Badge>, "variant"> & {
  status: StatusKind
  label?: React.ReactNode
}) {
  const variant = STATUS_VARIANT[status] ?? "secondary"
  return (
    <Badge variant={variant} className={className} data-status={status} {...props}>
      {label ?? status}
    </Badge>
  )
}

export { StatusBadge, STATUS_VARIANT }
export type { StatusKind }
