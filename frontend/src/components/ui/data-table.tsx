"use client"

import * as React from "react"

import { useRouter } from "@/i18n/navigation"
import { Skeleton } from "@/components/ui/skeleton"
import { cn } from "@/lib/utils"

export type ColumnDef<T> = {
  id: string
  header: React.ReactNode
  cell: (row: T) => React.ReactNode
  align?: "left" | "right" | "center"
  width?: string
  hideOnMobile?: boolean
  className?: string
  headerClassName?: string
}

export type DataTableProps<T> = {
  columns: ColumnDef<T>[]
  data: T[]
  keyField: (row: T) => React.Key
  getRowHref?: (row: T) => string | undefined
  onRowClick?: (row: T) => void
  loading?: boolean
  loadingRows?: number
  emptyState?: React.ReactNode
  rowActions?: (row: T) => React.ReactNode
  renderMobileCard?: (row: T) => React.ReactNode
  caption?: React.ReactNode
  className?: string
}

const ALIGN_CLASS: Record<NonNullable<ColumnDef<unknown>["align"]>, string> = {
  left: "text-left",
  right: "text-right",
  center: "text-center",
}

function DataTableSkeleton({
  rows = 5,
  columns,
}: {
  rows?: number
  columns: number
}) {
  return (
    <div data-slot="data-table-skeleton" className="overflow-hidden rounded-xl bg-card ring-1 ring-foreground/10">
      <div className="hidden md:block">
        <table className="w-full text-sm">
          <thead className="bg-muted/50">
            <tr>
              {Array.from({ length: columns }).map((_, i) => (
                <th key={i} className="px-4 py-3 text-left">
                  <Skeleton className="h-3 w-20" />
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {Array.from({ length: rows }).map((_, r) => (
              <tr key={r} className="border-b border-border last:border-0">
                {Array.from({ length: columns }).map((_, c) => (
                  <td key={c} className="px-4 py-3">
                    <Skeleton className="h-4 w-full" />
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="space-y-2 p-3 md:hidden">
        {Array.from({ length: rows }).map((_, r) => (
          <Skeleton key={r} className="h-20 w-full rounded-lg" />
        ))}
      </div>
    </div>
  )
}

function DataTable<T>({
  columns,
  data,
  keyField,
  getRowHref,
  onRowClick,
  loading,
  loadingRows = 5,
  emptyState,
  rowActions,
  renderMobileCard,
  caption,
  className,
}: DataTableProps<T>) {
  const router = useRouter()

  if (loading) {
    return (
      <DataTableSkeleton
        rows={loadingRows}
        columns={columns.length + (rowActions ? 1 : 0)}
      />
    )
  }

  if (data.length === 0 && emptyState) {
    return <>{emptyState}</>
  }

  const visibleColumns = columns

  return (
    <div
      data-slot="data-table"
      className={cn(
        "overflow-hidden rounded-xl bg-card ring-1 ring-foreground/10",
        className,
      )}
    >
      <div className="hidden md:block">
        <table className="w-full text-sm">
          {caption ? <caption className="sr-only">{caption}</caption> : null}
          <thead className="bg-muted/50">
            <tr>
              {visibleColumns.map((col) => (
                <th
                  key={col.id}
                  scope="col"
                  style={col.width ? { width: col.width } : undefined}
                  className={cn(
                    "px-4 py-3 text-xs font-medium uppercase tracking-wide text-muted-foreground",
                    col.align && ALIGN_CLASS[col.align],
                    !col.align && "text-left",
                    col.hideOnMobile && "hidden md:table-cell",
                    col.headerClassName,
                  )}
                >
                  {col.header}
                </th>
              ))}
              {rowActions ? (
                <th scope="col" className="px-4 py-3 text-right">
                  <span className="sr-only">Actions</span>
                </th>
              ) : null}
            </tr>
          </thead>
          <tbody>
            {data.map((row) => {
              const href = getRowHref?.(row)
              const clickable = Boolean(href || onRowClick)
              return (
                <tr
                  key={keyField(row)}
                  data-clickable={clickable || undefined}
                  className={cn(
                    "border-b border-border last:border-0",
                    clickable && "cursor-pointer hover:bg-muted/50",
                  )}
                  onClick={
                    href
                      ? () => router.push(href)
                      : onRowClick
                        ? () => onRowClick(row)
                        : undefined
                  }
                  onAuxClick={
                    href
                      ? (e) => { if (e.button === 1) { e.preventDefault(); window.open(href, "_blank") } }
                      : undefined
                  }
                >
                  {visibleColumns.map((col) => (
                    <td
                      key={col.id}
                      className={cn(
                        "px-4 py-3 text-foreground",
                        col.align && ALIGN_CLASS[col.align],
                        col.hideOnMobile && "hidden md:table-cell",
                        col.className,
                      )}
                    >
                      {col.cell(row)}
                    </td>
                  ))}
                  {rowActions ? (
                    <td className="px-4 py-3 text-right" onClick={(e) => e.stopPropagation()}>
                      {rowActions(row)}
                    </td>
                  ) : null}
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
      {renderMobileCard ? (
        <div className="space-y-2 p-3 md:hidden">
          {data.map((row) => (
            <React.Fragment key={keyField(row)}>{renderMobileCard(row)}</React.Fragment>
          ))}
        </div>
      ) : null}
    </div>
  )
}

export { DataTable, DataTableSkeleton }
