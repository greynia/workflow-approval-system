"use client";

import { useTranslations } from "next-intl";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { useUnsavedChangesStore } from "@/stores/unsaved-changes-store";

/**
 * Global confirm dialog mounted once per dashboard layout. Subscribes to the
 * shared store: opens whenever a navigation trigger queues a pending action,
 * closes after the user confirms or cancels.
 */
export function UnsavedChangesDialog() {
  const t = useTranslations("Common.UnsavedChanges");
  const pending = useUnsavedChangesStore((s) => s.pending);
  const confirm = useUnsavedChangesStore((s) => s.confirm);
  const cancel = useUnsavedChangesStore((s) => s.cancel);

  return (
    <Dialog
      open={pending !== null}
      onOpenChange={(open) => {
        if (!open) cancel();
      }}
    >
      <DialogContent showCloseButton={false}>
        <DialogHeader>
          <DialogTitle>{t("Title")}</DialogTitle>
          <DialogDescription>{t("Description")}</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="outline" onClick={cancel}>
            {t("Stay")}
          </Button>
          <Button variant="destructive" onClick={confirm}>
            {t("Leave")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
