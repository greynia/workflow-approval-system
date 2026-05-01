import * as React from "react"

import { cn } from "@/lib/utils"

function Section({
  title,
  description,
  actions,
  children,
  className,
  contentClassName,
  ...props
}: React.ComponentProps<"section"> & {
  title?: React.ReactNode
  description?: React.ReactNode
  actions?: React.ReactNode
  contentClassName?: string
}) {
  const hasHeader = title || description || actions
  return (
    <section
      data-slot="section"
      className={cn(
        "flex flex-col gap-3 rounded-xl bg-card p-4 ring-1 ring-foreground/10 sm:p-5",
        className,
      )}
      {...props}
    >
      {hasHeader ? (
        <header className="flex items-start justify-between gap-3">
          <div className="flex flex-col gap-0.5">
            {title ? (
              <h2 className="text-base font-semibold text-foreground">{title}</h2>
            ) : null}
            {description ? (
              <p className="text-sm text-muted-foreground">{description}</p>
            ) : null}
          </div>
          {actions ? <div className="shrink-0">{actions}</div> : null}
        </header>
      ) : null}
      <div className={cn("flex flex-col gap-3", contentClassName)}>{children}</div>
    </section>
  )
}

export { Section }
