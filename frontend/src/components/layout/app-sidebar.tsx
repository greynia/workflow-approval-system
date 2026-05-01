"use client"

import * as React from "react"
import type { LucideIcon } from "lucide-react"

import { Link, usePathname } from "@/i18n/navigation"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Sheet,
  SheetContent,
  SheetTitle,
} from "@/components/ui/sheet"
import { cn } from "@/lib/utils"

export type SidebarItem = {
  id: string
  label: string
  href: string
  icon?: LucideIcon
  badge?: number | string | null
  matchPaths?: string[]
}

export type AppSidebarProps = {
  items: SidebarItem[]
  open: boolean
  onOpenChange: (open: boolean) => void
  brand?: React.ReactNode
  footer?: React.ReactNode
  loading?: boolean
  pathname?: string
}

function matchesHref(href: string, pathname: string): boolean {
  return pathname === href || pathname.startsWith(href + "/")
}

function isActive(item: SidebarItem, pathname: string): boolean {
  if (matchesHref(item.href, pathname)) return true
  return item.matchPaths?.some((p) => matchesHref(p, pathname)) ?? false
}

function SidebarNavContent({
  items,
  pathname,
  onItemClick,
  loading,
}: {
  items: SidebarItem[]
  pathname: string
  onItemClick?: () => void
  loading?: boolean
}) {
  if (loading) {
    return (
      <nav className="flex-1 space-y-1 overflow-y-auto p-3">
        <Skeleton className="h-9 w-full" />
        <Skeleton className="h-9 w-full" />
        <Skeleton className="h-9 w-full" />
      </nav>
    )
  }

  return (
    <nav className="flex-1 space-y-0.5 overflow-y-auto p-3">
      {items.map((item) => {
        const active = isActive(item, pathname)
        const Icon = item.icon
        return (
          <Link
            key={item.id}
            href={item.href}
            onClick={onItemClick}
            data-slot="sidebar-item"
            data-active={active || undefined}
            className={cn(
              "flex items-center justify-between rounded-lg px-3 py-2 text-sm font-medium transition-colors",
              active
                ? "bg-primary text-primary-foreground"
                : "text-muted-foreground hover:bg-muted hover:text-foreground",
            )}
          >
            <span className="flex items-center gap-2">
              {Icon ? <Icon className="size-4 shrink-0" aria-hidden /> : null}
              {item.label}
            </span>
            {item.badge != null && item.badge !== 0 ? (
              <span
                className={cn(
                  "rounded-full px-1.5 py-0.5 text-xs tabular-nums",
                  active ? "bg-white/20 text-current" : "bg-muted text-muted-foreground",
                )}
              >
                {item.badge}
              </span>
            ) : null}
          </Link>
        )
      })}
    </nav>
  )
}

function AppSidebar({
  items,
  open,
  onOpenChange,
  brand,
  footer,
  loading,
  pathname: pathnameProp,
}: AppSidebarProps) {
  const routerPathname = usePathname()
  const pathname = pathnameProp ?? routerPathname

  const brandBlock = brand ? (
    <div className="flex h-14 shrink-0 items-center border-b border-border px-4">
      {brand}
    </div>
  ) : null

  const navContent = (
    <SidebarNavContent
      items={items}
      pathname={pathname}
      onItemClick={() => onOpenChange(false)}
      loading={loading}
    />
  )

  const footerBlock = footer ? (
    <div className="shrink-0 border-t border-border p-3">{footer}</div>
  ) : null

  return (
    <>
      {/* Desktop — always-visible static sidebar */}
      <aside
        data-slot="app-sidebar"
        className="hidden w-64 shrink-0 flex-col border-r border-border bg-card md:flex"
      >
        {brandBlock}
        {navContent}
        {footerBlock}
      </aside>

      {/* Mobile — Sheet drawer */}
      <Sheet open={open} onOpenChange={onOpenChange}>
        <SheetContent side="left" showCloseButton={false} className="w-64 gap-0 p-0">
          <SheetTitle className="sr-only">Navigation</SheetTitle>
          {brandBlock}
          {navContent}
          {footerBlock}
        </SheetContent>
      </Sheet>
    </>
  )
}

export { AppSidebar }
