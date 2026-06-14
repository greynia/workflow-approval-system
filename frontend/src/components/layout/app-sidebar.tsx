"use client"

import * as React from "react"
import type { LucideIcon } from "lucide-react"
import { ChevronsLeft, ChevronsRight } from "lucide-react"

import { Link, usePathname } from "@/i18n/navigation"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Sheet,
  SheetContent,
  SheetTitle,
} from "@/components/ui/sheet"
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip"
import { cn } from "@/lib/utils"

export type SidebarItem = {
  /** Stable identifier used as React key. */
  id: string
  /** Display text shown when expanded and inside the tooltip when collapsed. */
  label: string
  /** Target route. Item is marked active when pathname equals this or starts with `${href}/`. */
  href: string
  /** Optional Lucide icon. Required for the collapsed (icon-only) mode to be meaningful. */
  icon?: LucideIcon
  /** Optional count or short label. Falsy values (`null`, `undefined`, `0`) hide the badge. */
  badge?: number | string | null
  /** Additional pathnames that should also activate this item (e.g. nested admin routes). */
  matchPaths?: string[]
}

export type AppSidebarProps = {
  /** Navigation entries to render. Pass an empty array together with `loading` to show skeletons. */
  items: SidebarItem[]
  /** Mobile drawer open state. Ignored on desktop (≥ md breakpoint). */
  open: boolean
  /** Mobile drawer state setter. Fired on overlay tap and after selecting an item. */
  onOpenChange: (open: boolean) => void
  /**
   * Optional click interceptor invoked before the `<Link>` navigates. Calling
   * `event.preventDefault()` cancels navigation — used by the dashboard layout
   * to route clicks through the unsaved-changes guard.
   */
  onItemNavigate?: (item: SidebarItem, event: React.MouseEvent<HTMLAnchorElement>) => void
  /** Top-of-sidebar header content (logo / app name). Truncates when collapsed. */
  brand?: React.ReactNode
  /** Bottom block rendered above the collapse toggle. Hidden in collapsed mode. */
  footer?: React.ReactNode
  /** When true, renders skeleton placeholders instead of `items`. */
  loading?: boolean
  /** Override the auto-detected pathname. Mainly for Storybook/testing; production relies on the internal `usePathname()`. */
  pathname?: string
  /** Whether the desktop sidebar is in narrow icon-only mode. Mobile drawer ignores this. */
  collapsed?: boolean
  /** Setter for `collapsed`. When omitted no toggle button is rendered, leaving the sidebar at a fixed width. */
  onCollapsedChange?: (collapsed: boolean) => void
  /** aria-label for the toggle while the sidebar is expanded. Defaults to `"Collapse sidebar"`. */
  collapseLabel?: string
  /** aria-label for the toggle while the sidebar is collapsed. Defaults to `"Expand sidebar"`. */
  expandLabel?: string
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
  onItemNavigate,
  loading,
  collapsed = false,
}: {
  items: SidebarItem[]
  pathname: string
  onItemClick?: () => void
  onItemNavigate?: (item: SidebarItem, event: React.MouseEvent<HTMLAnchorElement>) => void
  loading?: boolean
  collapsed?: boolean
}) {
  if (loading) {
    return (
      <nav className="flex-1 space-y-1 overflow-y-auto p-3">
        <Skeleton className={cn("h-9", collapsed ? "w-10" : "w-full")} />
        <Skeleton className={cn("h-9", collapsed ? "w-10" : "w-full")} />
        <Skeleton className={cn("h-9", collapsed ? "w-10" : "w-full")} />
      </nav>
    )
  }

  return (
    <nav
      className={cn(
        "flex-1 space-y-0.5 overflow-y-auto",
        collapsed ? "p-2" : "p-3",
      )}
    >
      {items.map((item) => {
        const active = isActive(item, pathname)
        const Icon = item.icon
        const hasBadge = item.badge != null && item.badge !== 0

        const link = (
          <Link
            key={item.id}
            href={item.href}
            onClick={(event) => {
              onItemNavigate?.(item, event)
              if (event.defaultPrevented) return
              onItemClick?.()
            }}
            data-slot="sidebar-item"
            data-active={active || undefined}
            aria-label={collapsed ? item.label : undefined}
            className={cn(
              "relative flex items-center rounded-lg text-sm font-medium transition-colors",
              collapsed
                ? "h-10 w-10 justify-center"
                : "justify-between px-3 py-2",
              active
                ? "bg-primary text-primary-foreground"
                : "text-muted-foreground hover:bg-muted hover:text-foreground",
            )}
          >
            {collapsed ? (
              <>
                {Icon ? <Icon className="size-4" aria-hidden /> : null}
                {hasBadge ? (
                  <span
                    aria-hidden
                    className={cn(
                      "absolute top-1.5 right-1.5 size-1.5 rounded-full",
                      active ? "bg-primary-foreground" : "bg-primary",
                    )}
                  />
                ) : null}
              </>
            ) : (
              <>
                <span className="flex items-center gap-2">
                  {Icon ? <Icon className="size-4 shrink-0" aria-hidden /> : null}
                  {item.label}
                </span>
                {hasBadge ? (
                  <span
                    className={cn(
                      "rounded-full px-1.5 py-0.5 text-xs tabular-nums",
                      active
                        ? "bg-white/20 text-current"
                        : "bg-muted text-muted-foreground",
                    )}
                  >
                    {item.badge}
                  </span>
                ) : null}
              </>
            )}
          </Link>
        )

        if (!collapsed) return link

        return (
          <Tooltip key={item.id}>
            <TooltipTrigger asChild>{link}</TooltipTrigger>
            <TooltipContent side="right">
              <span className="font-medium">{item.label}</span>
              {hasBadge ? (
                <span className="ml-2 text-muted">{item.badge}</span>
              ) : null}
            </TooltipContent>
          </Tooltip>
        )
      })}
    </nav>
  )
}

function AppSidebar({
  items,
  open,
  onOpenChange,
  onItemNavigate,
  brand,
  footer,
  loading,
  pathname: pathnameProp,
  collapsed = false,
  onCollapsedChange,
  collapseLabel = "Collapse sidebar",
  expandLabel = "Expand sidebar",
}: AppSidebarProps) {
  const routerPathname = usePathname()
  const pathname = pathnameProp ?? routerPathname

  const desktopBrandBlock = brand ? (
    <div
      className={cn(
        "flex h-14 shrink-0 items-center overflow-hidden border-b border-border",
        collapsed ? "justify-center px-2" : "px-4",
      )}
    >
      {brand}
    </div>
  ) : null

  const mobileBrandBlock = brand ? (
    <div className="flex h-14 shrink-0 items-center border-b border-border px-4">
      {brand}
    </div>
  ) : null

  const collapseToggle = onCollapsedChange ? (
    <Button
      type="button"
      variant="ghost"
      size="icon"
      onClick={() => onCollapsedChange(!collapsed)}
      aria-label={collapsed ? expandLabel : collapseLabel}
      className="hidden md:inline-flex"
    >
      {collapsed ? (
        <ChevronsRight className="size-4" aria-hidden />
      ) : (
        <ChevronsLeft className="size-4" aria-hidden />
      )}
    </Button>
  ) : null

  const desktopFooterBlock =
    footer || collapseToggle ? (
      <div
        className={cn(
          "flex shrink-0 border-t border-border",
          collapsed
            ? "flex-col items-center gap-2 p-2"
            : "items-center justify-between gap-2 p-3",
        )}
      >
        {!collapsed && footer ? <div className="min-w-0 flex-1">{footer}</div> : null}
        {collapseToggle}
      </div>
    ) : null

  const mobileFooterBlock = footer ? (
    <div className="shrink-0 border-t border-border p-3">{footer}</div>
  ) : null

  return (
    <TooltipProvider>
      {/* Desktop — always-visible static sidebar */}
      <aside
        data-slot="app-sidebar"
        data-collapsed={collapsed || undefined}
        className={cn(
          "hidden shrink-0 flex-col border-r border-border bg-card transition-[width] duration-200 ease-out md:flex",
          collapsed ? "md:w-16" : "md:w-64",
        )}
      >
        {desktopBrandBlock}
        <SidebarNavContent
          items={items}
          pathname={pathname}
          loading={loading}
          collapsed={collapsed}
          onItemNavigate={onItemNavigate}
        />
        {desktopFooterBlock}
      </aside>

      {/* Mobile — Sheet drawer (always full width, collapse N/A) */}
      <Sheet open={open} onOpenChange={onOpenChange}>
        <SheetContent
          side="left"
          showCloseButton={false}
          className="w-64 gap-0 p-0"
        >
          <SheetTitle className="sr-only">Navigation</SheetTitle>
          {mobileBrandBlock}
          <SidebarNavContent
            items={items}
            pathname={pathname}
            onItemClick={() => onOpenChange(false)}
            onItemNavigate={onItemNavigate}
            loading={loading}
          />
          {mobileFooterBlock}
        </SheetContent>
      </Sheet>
    </TooltipProvider>
  )
}

export { AppSidebar }
