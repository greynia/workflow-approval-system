"use client";

import * as Toast from "@radix-ui/react-toast";
import { useToastStore } from "@/stores/toast-store";

const variantStyles = {
  success: "border-green-500 bg-green-50 text-green-900",
  error: "border-red-500 bg-red-50 text-red-900",
  info: "border-zinc-500 bg-zinc-50 text-zinc-900",
};

export function Toaster() {
  const toasts = useToastStore((s) => s.toasts);
  const removeToast = useToastStore((s) => s.removeToast);

  return (
    <Toast.Provider swipeDirection="right" duration={4000}>
      {toasts.map((toast) => (
        <Toast.Root
          key={toast.id}
          className={`rounded-md border p-4 shadow-md ${variantStyles[toast.variant]}`}
          onOpenChange={(open) => {
            if (!open) removeToast(toast.id);
          }}
        >
          <Toast.Title className="text-sm font-semibold">
            {toast.title}
          </Toast.Title>
          {toast.description && (
            <Toast.Description className="mt-1 text-sm opacity-80">
              {toast.description}
            </Toast.Description>
          )}
        </Toast.Root>
      ))}

      <Toast.Viewport className="fixed bottom-4 right-4 z-50 flex w-80 flex-col gap-2" />
    </Toast.Provider>
  );
}
