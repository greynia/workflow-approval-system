import * as React from "react"

import { cn } from "@/lib/utils"

function DescriptionList({
  className,
  columns = 2,
  ...props
}: React.ComponentProps<"dl"> & { columns?: 1 | 2 | 3 }) {
  const gridClass =
    columns === 1
      ? "grid-cols-1"
      : columns === 3
        ? "grid-cols-1 sm:grid-cols-2 lg:grid-cols-3"
        : "grid-cols-1 sm:grid-cols-2"
  return (
    <dl
      data-slot="description-list"
      className={cn("grid gap-x-6 gap-y-3", gridClass, className)}
      {...props}
    />
  )
}

function DescriptionTerm({ className, ...props }: React.ComponentProps<"dt">) {
  return (
    <dt
      data-slot="description-term"
      className={cn("text-xs font-medium uppercase tracking-wide text-muted-foreground", className)}
      {...props}
    />
  )
}

function DescriptionDetail({ className, ...props }: React.ComponentProps<"dd">) {
  return (
    <dd
      data-slot="description-detail"
      className={cn("mt-1 text-sm text-foreground", className)}
      {...props}
    />
  )
}

function DescriptionItem({
  term,
  children,
  className,
  ...props
}: React.ComponentProps<"div"> & { term: React.ReactNode }) {
  return (
    <div data-slot="description-item" className={cn("flex flex-col", className)} {...props}>
      <DescriptionTerm>{term}</DescriptionTerm>
      <DescriptionDetail>{children}</DescriptionDetail>
    </div>
  )
}

export { DescriptionList, DescriptionTerm, DescriptionDetail, DescriptionItem }
