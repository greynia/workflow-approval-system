"use client"

import * as React from "react"
import { Menu } from "lucide-react"

import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

function AppHeader({
  onToggleSidebar,
  toggleLabel,
  children,
  className,
}: {
  onToggleSidebar?: () => void
  toggleLabel?: string
  children?: React.ReactNode
  className?: string
}) {
  return (
    <header
      data-slot="app-header"
      className={cn(
        "sticky top-0 z-20 flex h-14 shrink-0 items-center justify-between gap-2 border-b border-border bg-background px-4",
        className,
      )}
    >
      {onToggleSidebar ? (
        <Button
          type="button"
          variant="ghost"
          size="icon"
          onClick={onToggleSidebar}
          aria-label={toggleLabel}
          className="md:hidden"
        >
          <Menu className="size-5" aria-hidden />
        </Button>
      ) : null}
      <div className="flex-1" />
      <div className="flex items-center gap-2">{children}</div>
    </header>
  )
}

export { AppHeader }
