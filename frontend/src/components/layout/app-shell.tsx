"use client"

import * as React from "react"

import { cn } from "@/lib/utils"

function AppShell({
  sidebar,
  header,
  children,
  className,
}: {
  /** Left rail content — typically `<AppSidebar>`. Layout assumes the node manages its own desktop/mobile responsive behavior. */
  sidebar: React.ReactNode
  /** Top bar content — typically `<AppHeader>`. Stays sticky inside the right-side flex column. */
  header: React.ReactNode
  /** Main page content rendered inside `<main>` with default padding (`p-4 sm:p-6`). */
  children: React.ReactNode
  /** Additional Tailwind classes merged with the outer flex container. */
  className?: string
}) {
  return (
    <div data-slot="app-shell" className={cn("flex min-h-screen bg-muted/40", className)}>
      {sidebar}
      <div className="flex min-w-0 flex-1 flex-col">
        {header}
        <main className="flex-1 p-4 sm:p-6">{children}</main>
      </div>
    </div>
  )
}

export { AppShell }
