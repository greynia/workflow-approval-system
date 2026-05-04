"use client";

import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import LeaveService from "@/services/leave.service";
import { useFormatDurationAsHours } from "@/lib/use-format-duration";
import { PageHeader } from "@/components/ui/page-header";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import {
  DescriptionList,
  DescriptionItem,
} from "@/components/ui/description-list";
import { EmptyState } from "@/components/ui/empty-state";
import { Skeleton } from "@/components/ui/skeleton";
import type { LeaveBalanceResponse } from "@/types/leave";

const WARNING_THRESHOLD = 50;
const DANGER_THRESHOLD = 80;

function getUsagePercent(used: number, quota: number): number {
  if (quota <= 0) return 0;
  return Math.min(100, Math.round((used / quota) * 100));
}

function getIndicatorClass(percent: number): string {
  if (percent >= DANGER_THRESHOLD) return "bg-destructive";
  if (percent >= WARNING_THRESHOLD) return "bg-warning";
  return "bg-primary";
}

export default function LeaveBalancesPage() {
  const t = useTranslations("Balances");
  const tLeaveType = useTranslations("Requests.List.LeaveType");
  const formatDuration = useFormatDurationAsHours();
  const year = useMemo(() => new Date().getFullYear(), []);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["requests", "balance", year],
    queryFn: () => LeaveService.getBalances(year),
  });

  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-6">
      <PageHeader title={t("Title")} description={t("Description", { year })} />

      {isError ? (
        <EmptyState title={t("Error")} />
      ) : isLoading ? (
        <BalanceCardGrid>
          {Array.from({ length: 4 }).map((_, idx) => (
            <BalanceCardSkeleton key={idx} />
          ))}
        </BalanceCardGrid>
      ) : !data || data.length === 0 ? (
        <EmptyState title={t("Empty")} />
      ) : (
        <BalanceCardGrid>
          {data.map((row) => (
            <BalanceCard
              key={row.leaveType}
              row={row}
              leaveTypeLabel={tLeaveType(row.leaveType)}
              labels={{
                quota: t("Table.Quota"),
                used: t("Table.Used"),
                remaining: t("Table.Remaining"),
                usageRate: t("UsageRate"),
              }}
              formatDuration={formatDuration}
            />
          ))}
        </BalanceCardGrid>
      )}
    </div>
  );
}

function BalanceCardGrid({ children }: { children: React.ReactNode }) {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {children}
    </div>
  );
}

interface BalanceCardLabels {
  quota: string;
  used: string;
  remaining: string;
  usageRate: string;
}

function BalanceCard({
  row,
  leaveTypeLabel,
  labels,
  formatDuration,
}: {
  row: LeaveBalanceResponse;
  leaveTypeLabel: string;
  labels: BalanceCardLabels;
  formatDuration: (minutes: number) => string;
}) {
  const usagePercent = getUsagePercent(row.usedMinutes, row.quotaMinutes);
  const indicatorClass = getIndicatorClass(usagePercent);

  return (
    <Card>
      <CardHeader>
        <CardTitle>{leaveTypeLabel}</CardTitle>
        <CardDescription>
          <span className="text-2xl font-semibold text-foreground">
            {formatDuration(row.remainingMinutes)}
          </span>
          <span className="text-muted-foreground">
            {" / "}
            {formatDuration(row.quotaMinutes)}
          </span>
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <span>{labels.usageRate}</span>
            <span className="font-medium text-foreground">{usagePercent}%</span>
          </div>
          <Progress value={usagePercent} indicatorClassName={indicatorClass} />
        </div>
        <DescriptionList columns={3} className="gap-x-3 gap-y-0">
          <DescriptionItem term={labels.quota}>
            {formatDuration(row.quotaMinutes)}
          </DescriptionItem>
          <DescriptionItem term={labels.used}>
            {formatDuration(row.usedMinutes)}
          </DescriptionItem>
          <DescriptionItem term={labels.remaining}>
            {formatDuration(row.remainingMinutes)}
          </DescriptionItem>
        </DescriptionList>
      </CardContent>
    </Card>
  );
}

function BalanceCardSkeleton() {
  return (
    <Card>
      <CardHeader>
        <Skeleton className="h-5 w-24" />
        <Skeleton className="mt-2 h-7 w-32" />
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <Skeleton className="h-1 w-full" />
        <div className="grid grid-cols-3 gap-3">
          <Skeleton className="h-10" />
          <Skeleton className="h-10" />
          <Skeleton className="h-10" />
        </div>
      </CardContent>
    </Card>
  );
}
