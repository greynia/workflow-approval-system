"use client";

import * as React from "react";
import { AlertTriangle, CheckCircle } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useUnsavedChangesGuard } from "@/lib/use-unsaved-changes-guard";
import { useUnsavedChangesStore } from "@/stores/unsaved-changes-store";

export type ApprovalDialogVariant = "approve" | "reject";

export type ApprovalDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  variant: ApprovalDialogVariant;
  title: React.ReactNode;
  description: React.ReactNode;
  applicantSummary: React.ReactNode;
  confirmLabel: React.ReactNode;
  cancelLabel: React.ReactNode;
  isPending?: boolean;
  errorMessage?: string | null;
  // Reject-only props (ignored when variant is "approve")
  commentLabel?: React.ReactNode;
  commentPlaceholder?: string;
  commentRequiredError?: string;
  onConfirm: (comment: string | null) => void;
};

function ApprovalDialog({
  open,
  onOpenChange,
  variant,
  title,
  description,
  applicantSummary,
  confirmLabel,
  cancelLabel,
  isPending = false,
  errorMessage,
  commentLabel,
  commentPlaceholder,
  commentRequiredError,
  onConfirm,
}: ApprovalDialogProps) {
  const [comment, setComment] = React.useState("");
  const [commentError, setCommentError] = React.useState<string | null>(null);

  // Reset internal state whenever the dialog closes so a re-open starts fresh.
  React.useEffect(() => {
    if (!open) {
      setComment("");
      setCommentError(null);
    }
  }, [open]);

  const isRejectDirty =
    open && variant === "reject" && comment.trim().length > 0;
  useUnsavedChangesGuard({ when: isRejectDirty });

  const requestClose = React.useCallback(() => {
    if (isRejectDirty) {
      useUnsavedChangesStore.getState().request(() => onOpenChange(false));
      return;
    }
    onOpenChange(false);
  }, [isRejectDirty, onOpenChange]);

  function handleConfirm() {
    if (variant === "approve") {
      onConfirm(null);
      return;
    }
    const trimmed = comment.trim();
    if (!trimmed) {
      setCommentError(commentRequiredError ?? "Required");
      return;
    }
    onConfirm(trimmed);
  }

  const Icon = variant === "approve" ? CheckCircle : AlertTriangle;
  const iconClass =
    variant === "approve" ? "size-4 text-success" : "size-4 text-destructive";
  const confirmVariant = variant === "approve" ? "success" : "destructive";

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (!next) {
          requestClose();
          return;
        }
        onOpenChange(next);
      }}
    >
      <DialogContent>
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Icon className={iconClass} aria-hidden />
            {title}
          </DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>

        <div className="text-sm font-medium text-foreground">
          {applicantSummary}
        </div>

        {variant === "reject" ? (
          <div className="flex flex-col gap-1">
            {commentLabel ? (
              <label
                htmlFor="approval-dialog-comment"
                className="text-sm font-medium text-foreground"
              >
                {commentLabel}
              </label>
            ) : null}
            <textarea
              id="approval-dialog-comment"
              rows={3}
              placeholder={commentPlaceholder}
              value={comment}
              onChange={(e) => {
                setComment(e.target.value);
                if (commentError) setCommentError(null);
              }}
              aria-invalid={Boolean(commentError) || undefined}
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm placeholder:text-muted-foreground focus-visible:border-ring focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50 aria-invalid:border-destructive aria-invalid:ring-destructive/20"
            />
            {commentError ? (
              <p className="text-xs text-destructive">{commentError}</p>
            ) : null}
          </div>
        ) : null}

        {errorMessage ? (
          <p className="text-sm text-destructive">{errorMessage}</p>
        ) : null}

        <DialogFooter>
          <Button
            variant="outline"
            disabled={isPending}
            onClick={requestClose}
          >
            {cancelLabel}
          </Button>
          <Button
            variant={confirmVariant}
            disabled={isPending}
            onClick={handleConfirm}
          >
            {isPending ? "…" : confirmLabel}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export { ApprovalDialog };
