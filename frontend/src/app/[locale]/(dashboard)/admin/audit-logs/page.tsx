"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import AuditLogService from "@/services/audit-log.service";
import type { AuditLog, AuditLogFilters } from "@/types/audit-log";
import { PageHeader } from "@/components/ui/page-header";
import { DataTable, type ColumnDef } from "@/components/ui/data-table";
import { EmptyState } from "@/components/ui/empty-state";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import {
  AuditLogFilterBar,
  type AuditLogFilterLabels,
  type AuditLogFilterOption,
  type AuditLogFilterValues,
} from "@/components/audit/audit-log-filter-bar";

const DEFAULT_SIZE = 20;
const ENTITY_TYPE_VALUES = ["LEAVE_REQUEST"] as const;
const ACTION_VALUES = ["CREATE", "APPROVE", "REJECT", "CANCEL"] as const;

function renderDetailJson(detailJson: string): React.ReactNode {
  let parsed: Record<string, unknown>;
  try {
    parsed = JSON.parse(detailJson);
  } catch {
    return <span className="font-mono text-xs text-muted-foreground">{detailJson}</span>;
  }

  return (
    <div className="flex flex-col gap-2">
      {Object.entries(parsed).map(([key, value]) => {
        if (value === null || value === undefined) return null;
        const display =
          typeof value === "object" ? JSON.stringify(value) : String(value);
        return (
          <div key={key} className="flex justify-between gap-4 text-xs">
            <span className="shrink-0 text-muted-foreground">{key}</span>
            <span className="text-right font-mono text-foreground">{display}</span>
          </div>
        );
      })}
    </div>
  );
}

export default function AuditLogsPage() {
  const t = useTranslations("AuditLog");
  const [selectedLog, setSelectedLog] = useState<AuditLog | null>(null);

  const entityTypeOptions: AuditLogFilterOption[] = ENTITY_TYPE_VALUES.map(
    (value) => ({ value, label: t(`Filter.EntityTypeOption.${value}`) }),
  );
  const actionOptions: AuditLogFilterOption[] = ACTION_VALUES.map((value) => ({
    value,
    label: t(`Filter.ActionOption.${value}`),
  }));

  const [filters, setFilters] = useState<AuditLogFilters>({
    page: 0,
    size: DEFAULT_SIZE,
  });

  function applyFilterValues(next: AuditLogFilterValues) {
    setFilters((prev) => ({
      ...prev,
      ...next,
      page: 0,
    }));
  }

  function handlePageChange(direction: "prev" | "next") {
    setFilters((prev) => ({
      ...prev,
      page: direction === "prev" ? prev.page - 1 : prev.page + 1,
    }));
  }

  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin", "audit-logs", filters],
    queryFn: () => AuditLogService.getAuditLogs(filters),
  });

  const filterValues: AuditLogFilterValues = {
    entityType: filters.entityType,
    action: filters.action,
    actorName: filters.actorName,
    createdFrom: filters.createdFrom,
    createdTo: filters.createdTo,
  };

  const filterLabels: AuditLogFilterLabels = {
    entityType: t("Filter.EntityType"),
    action: t("Filter.Action"),
    actorName: t("Filter.ActorName"),
    createdFrom: t("Filter.CreatedFrom"),
    createdTo: t("Filter.CreatedTo"),
    entityTypePlaceholder: t("Filter.Placeholder.EntityType"),
    actionPlaceholder: t("Filter.Placeholder.Action"),
    actorNamePlaceholder: t("Filter.Placeholder.ActorName"),
  };

  const columns: ColumnDef<AuditLog>[] = [
    {
      id: "time",
      header: t("Table.Time"),
      cell: (row) => (
        <span className="text-muted-foreground">
          {new Date(row.createdAt).toLocaleString()}
        </span>
      ),
    },
    {
      id: "actor",
      header: t("Table.Actor"),
      cell: (row) => (
        <span className="font-medium text-foreground">{row.actorName}</span>
      ),
    },
    { id: "action", header: t("Table.Action"), cell: (row) => row.action },
    {
      id: "entityType",
      header: t("Table.EntityType"),
      cell: (row) => row.entityType,
      hideOnMobile: true,
    },
    {
      id: "entityId",
      header: t("Table.EntityId"),
      cell: (row) => row.entityId,
      hideOnMobile: true,
    },
    {
      id: "detail",
      header: t("Table.Detail"),
      cell: () => (
        <span className="text-xs text-muted-foreground">
          {t("Table.Detail")} →
        </span>
      ),
    },
  ];

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-6">
      <PageHeader title={t("Title")} description={t("Description")} />

      <AuditLogFilterBar
        value={filterValues}
        onChange={applyFilterValues}
        entityTypeOptions={entityTypeOptions}
        actionOptions={actionOptions}
        labels={filterLabels}
      />

      {isError ? (
        <EmptyState title={t("Error")} />
      ) : (
        <DataTable
          columns={columns}
          data={data?.items ?? []}
          keyField={(row) => row.id}
          loading={isLoading}
          loadingRows={6}
          onRowClick={(row) => setSelectedLog(row)}
          emptyState={<EmptyState title={t("Empty")} />}
          renderMobileCard={(row) => (
            <button
              type="button"
              onClick={() => setSelectedLog(row)}
              className="w-full rounded-lg border border-border bg-card p-4 text-left transition-colors hover:bg-muted/50"
            >
              <div className="flex items-start justify-between gap-2">
                <span className="font-medium text-foreground">{row.actorName}</span>
                <Badge variant="secondary">{row.action}</Badge>
              </div>
              <p className="mt-1 text-sm text-muted-foreground">
                {row.entityType} #{row.entityId}
              </p>
              <p className="mt-1 text-xs text-muted-foreground">
                {new Date(row.createdAt).toLocaleString()}
              </p>
            </button>
          )}
        />
      )}

      {data && data.totalPages > 1 ? (
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <span>
            {t("Pagination.PageInfo", {
              current: data.currentPage + 1,
              total: data.totalPages,
              count: data.totalCount,
            })}
          </span>
          <div className="flex gap-2">
            <Button
              size="sm"
              variant="outline"
              onClick={() => handlePageChange("prev")}
              disabled={data.currentPage === 0}
            >
              {t("Pagination.Previous")}
            </Button>
            <Button
              size="sm"
              variant="outline"
              onClick={() => handlePageChange("next")}
              disabled={data.currentPage + 1 >= data.totalPages}
            >
              {t("Pagination.Next")}
            </Button>
          </div>
        </div>
      ) : null}

      <Sheet
        open={selectedLog !== null}
        onOpenChange={(open) => !open && setSelectedLog(null)}
      >
        {selectedLog ? (
          <SheetContent>
            <SheetHeader>
              <SheetTitle>{t("Drawer.Title")}</SheetTitle>
              <SheetDescription>
                {selectedLog.entityType} #{selectedLog.entityId}
              </SheetDescription>
            </SheetHeader>

            <div className="flex-1 overflow-y-auto px-4">
              <div className="flex flex-col gap-3">
                <DrawerField
                  label={t("Drawer.Fields.Time")}
                  value={new Date(selectedLog.createdAt).toLocaleString()}
                />
                <DrawerField
                  label={t("Drawer.Fields.Actor")}
                  value={selectedLog.actorName}
                />
                <DrawerField
                  label={t("Drawer.Fields.Action")}
                  value={selectedLog.action}
                />
                <DrawerField
                  label={t("Drawer.Fields.EntityType")}
                  value={selectedLog.entityType}
                />
                <DrawerField
                  label={t("Drawer.Fields.EntityId")}
                  value={String(selectedLog.entityId)}
                />
              </div>

              <hr className="my-4 border-border" />

              <div>
                <p className="mb-2 text-xs font-medium uppercase tracking-wide text-muted-foreground">
                  {t("Drawer.Fields.Detail")}
                </p>
                {renderDetailJson(selectedLog.detailJson)}
              </div>
            </div>

            {selectedLog.entityType === "LEAVE_REQUEST" ? (
              <SheetFooter>
                <Button asChild variant="link" className="self-start px-0">
                  <Link href={`/admin/requests/${selectedLog.entityId}`}>
                    {t("Drawer.ViewRequest")} →
                  </Link>
                </Button>
              </SheetFooter>
            ) : null}
          </SheetContent>
        ) : null}
      </Sheet>
    </div>
  );
}

function DrawerField({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-4 text-sm">
      <span className="shrink-0 text-muted-foreground">{label}</span>
      <span className="text-right font-medium text-foreground">{value}</span>
    </div>
  );
}
