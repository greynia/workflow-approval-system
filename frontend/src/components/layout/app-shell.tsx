"use client"

import * as React from "react"

import { cn } from "@/lib/utils"

function AppShell({
  sidebar,
  header,
  children,
  className,
}: {
  sidebar: React.ReactNode
  header: React.ReactNode
  children: React.ReactNode
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
