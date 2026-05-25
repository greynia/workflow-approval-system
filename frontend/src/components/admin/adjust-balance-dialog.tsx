"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import LeaveService from "@/services/leave.service";
import { useToastStore } from "@/stores/toast-store";
import { Button } from "@/components/ui/button";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import type { AdminLeaveBalance } from "@/types/leave";

const MINUTES_PER_DAY = 480;

interface Props {
  open: boolean;
  balance: AdminLeaveBalance | null;
  onClose: () => void;
  onSuccess: () => void;
}

export function AdjustBalanceDialog({ open, balance, onClose, onSuccess }: Props) {
  const t = useTranslations("Admin.LeaveBalance");
  const toast = useToastStore();

  const [quotaDays, setQuotaDays] = useState("");
  const [reason, setReason] = useState("");
  const [trackedBalance, setTrackedBalance] = useState<AdminLeaveBalance | null>(null);

  // Reset the form whenever a different balance is selected. Done during render
  // (React's "adjusting state on a prop change" pattern) rather than in an effect,
  // which would trip react-hooks/set-state-in-effect and add an extra render pass.
  if (balance !== trackedBalance) {
    setTrackedBalance(balance);
    if (balance) {
      setQuotaDays(String(balance.quotaMinutes / MINUTES_PER_DAY));
      setReason("");
    }
  }

  const mutation = useMutation({
    mutationFn: ({
      id,
      quotaMinutes,
      reason: adjustReason,
    }: {
      id: number;
      quotaMinutes: number;
      reason: string;
    }) => LeaveService.adjustBalance(id, { quotaMinutes, reason: adjustReason }),
    onSuccess: () => {
      onSuccess();
      onClose();
    },
    onError: () => {
      toast.error(t("AdjustError"));
    },
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!balance) return;
    const quotaMinutes = Math.round(Number(quotaDays) * MINUTES_PER_DAY);
    mutation.mutate({ id: balance.id, quotaMinutes, reason });
  }

  return (
    <Sheet open={open} onOpenChange={(isOpen) => { if (!isOpen) onClose(); }}>
      <SheetContent>
        <SheetHeader>
          <SheetTitle>{t("AdjustTitle")}</SheetTitle>
          <SheetDescription>
            {balance?.employeeName} — {balance?.leaveType}
          </SheetDescription>
        </SheetHeader>

        <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-4 px-4">
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium" htmlFor="quota-days">
              {t("Fields.Quota")}
            </label>
            <input
              id="quota-days"
              type="number"
              min="0"
              step="0.5"
              className="rounded-md border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
              value={quotaDays}
              onChange={(e) => setQuotaDays(e.target.value)}
              required
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium" htmlFor="adjust-reason">
              {t("Fields.Reason")}
            </label>
            <input
              id="adjust-reason"
              type="text"
              className="rounded-md border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              required
            />
          </div>

          <SheetFooter className="mt-2">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              disabled={mutation.isPending}
            >
              {t("Actions.Cancel")}
            </Button>
            <Button type="submit" disabled={mutation.isPending || !reason.trim()}>
              {t("Actions.Submit")}
            </Button>
          </SheetFooter>
        </form>
      </SheetContent>
    </Sheet>
  );
}
