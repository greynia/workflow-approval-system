"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import LeaveService from "@/services/leave.service";
import { useToastStore } from "@/stores/toast-store";
import { PageHeader } from "@/components/ui/page-header";
import { Button } from "@/components/ui/button";
import { DataTable, type ColumnDef } from "@/components/ui/data-table";
import { EmptyState } from "@/components/ui/empty-state";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { AdjustBalanceDialog } from "@/components/admin/adjust-balance-dialog";
import type { AdminLeaveBalance, AdminLeaveBalanceFilters } from "@/types/leave";

const MINUTES_PER_DAY = 480;
const DEFAULT_SIZE = 20;
const CURRENT_YEAR = new Date().getFullYear();
const YEAR_OPTIONS = [CURRENT_YEAR - 1, CURRENT_YEAR, CURRENT_YEAR + 1];

function formatDays(minutes: number): string {
  return (minutes / MINUTES_PER_DAY).toFixed(1);
}

export default function AdminLeaveBalancesPage() {
  const t = useTranslations("Admin.LeaveBalance");
  const tLeaveType = useTranslations("Requests.Detail");
  const toast = useToastStore();
  const queryClient = useQueryClient();

  const [filters, setFilters] = useState<AdminLeaveBalanceFilters>({
    year: CURRENT_YEAR,
    page: 0,
    size: DEFAULT_SIZE,
  });
  const [employeeSearch, setEmployeeSearch] = useState("");
  const [adjustTarget, setAdjustTarget] = useState<AdminLeaveBalance | null>(null);
  const [showInitDialog, setShowInitDialog] = useState(false);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin", "leave-balances", filters],
    queryFn: () => LeaveService.getAdminBalances(filters),
  });

  const initMutation = useMutation({
    mutationFn: ({ year }: { year: number }) =>
      LeaveService.initYearBalances({ year }),
    onSuccess: (result, variables) => {
      queryClient.invalidateQueries({ queryKey: ["admin", "leave-balances"] });
      toast.success(
        t("InitSuccess", {
          initialized: result.initialized,
          skipped: result.skipped,
          year: variables.year,
        })
      );
      setShowInitDialog(false);
    },
    onError: () => {
      toast.error(t("InitError"));
    },
  });

  function applySearch() {
    setFilters((prev) => ({
      ...prev,
      page: 0,
    }));
  }

  function handlePageChange(direction: "prev" | "next") {
    setFilters((prev) => ({
      ...prev,
      page: direction === "prev" ? prev.page - 1 : prev.page + 1,
    }));
  }

  function invalidateBalances() {
    queryClient.invalidateQueries({ queryKey: ["admin", "leave-balances"] });
  }

  const columns: ColumnDef<AdminLeaveBalance>[] = [
    {
      id: "employee",
      header: t("Fields.Employee"),
      cell: (row) => <span className="font-medium">{row.employeeName}</span>,
    },
    {
      id: "leaveType",
      header: t("Fields.LeaveType"),
      cell: (row) => tLeaveType(`LeaveType.${row.leaveType}`),
    },
    {
      id: "quota",
      header: t("Fields.Quota"),
      align: "right",
      cell: (row) => `${formatDays(row.quotaMinutes)} 天`,
    },
    {
      id: "used",
      header: t("Fields.Used"),
      align: "right",
      hideOnMobile: true,
      cell: (row) => `${formatDays(row.usedMinutes)} 天`,
    },
    {
      id: "remaining",
      header: t("Fields.Remaining"),
      align: "right",
      cell: (row) => `${formatDays(row.remainingMinutes)} 天`,
    },
    {
      id: "updatedAt",
      header: t("Fields.UpdatedAt"),
      hideOnMobile: true,
      cell: (row) => (
        <span className="text-muted-foreground text-xs">
          {new Date(row.updatedAt).toLocaleDateString()}
        </span>
      ),
    },
  ];

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-6">
      <PageHeader
        title={t("Title")}
        actions={
          <Button
            size="sm"
            variant="outline"
            onClick={() => setShowInitDialog(true)}
          >
            {t("InitYear")}
          </Button>
        }
      />

      <div className="flex flex-wrap items-end gap-3">
        <div className="flex flex-col gap-1">
          <label className="text-sm font-medium text-muted-foreground">
            年度
          </label>
          <select
            className="rounded-md border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
            value={filters.year}
            onChange={(e) =>
              setFilters((prev) => ({
                ...prev,
                year: Number(e.target.value),
                page: 0,
              }))
            }
          >
            {YEAR_OPTIONS.map((y) => (
              <option key={y} value={y}>
                {y}
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-col gap-1">
          <label className="text-sm font-medium text-muted-foreground">
            {t("Fields.Employee")}
          </label>
          <input
            type="text"
            className="rounded-md border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
            placeholder="姓名搜尋"
            value={employeeSearch}
            onChange={(e) => setEmployeeSearch(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") applySearch();
            }}
          />
        </div>

        <Button size="sm" variant="outline" onClick={applySearch}>
          搜尋
        </Button>
      </div>

      {isError ? (
        <EmptyState title={t("Error")} />
      ) : (
        <DataTable
          columns={columns}
          data={data?.items ?? []}
          keyField={(row) => row.id}
          loading={isLoading}
          loadingRows={6}
          emptyState={<EmptyState title={t("Empty")} />}
          rowActions={(row) => (
            <Button
              size="sm"
              variant="ghost"
              onClick={() => setAdjustTarget(row)}
            >
              {t("Actions.Adjust")}
            </Button>
          )}
        />
      )}

      {data && data.totalPages > 1 ? (
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <span>
            第 {data.currentPage + 1} / {data.totalPages} 頁（共 {data.totalCount} 筆）
          </span>
          <div className="flex gap-2">
            <Button
              size="sm"
              variant="outline"
              onClick={() => handlePageChange("prev")}
              disabled={data.currentPage === 0}
            >
              上一頁
            </Button>
            <Button
              size="sm"
              variant="outline"
              onClick={() => handlePageChange("next")}
              disabled={data.currentPage + 1 >= data.totalPages}
            >
              下一頁
            </Button>
          </div>
        </div>
      ) : null}

      <AdjustBalanceDialog
        open={adjustTarget !== null}
        balance={adjustTarget}
        onClose={() => setAdjustTarget(null)}
        onSuccess={invalidateBalances}
      />

      <Dialog open={showInitDialog} onOpenChange={setShowInitDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t("InitConfirmTitle")}</DialogTitle>
            <DialogDescription>
              {t("InitConfirmDesc", { year: filters.year })}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setShowInitDialog(false)}
              disabled={initMutation.isPending}
            >
              取消
            </Button>
            <Button
              onClick={() => initMutation.mutate({ year: filters.year })}
              disabled={initMutation.isPending}
            >
              {t("InitYear")}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
