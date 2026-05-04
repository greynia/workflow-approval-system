import { create } from "zustand";

export type ToastVariant = "success" | "error" | "info";

export interface ToastMessage {
  id: string;
  title: string;
  description?: string;
  variant: ToastVariant;
}

interface ToastState {
  toasts: ToastMessage[];
  addToast: (toast: Omit<ToastMessage, "id">) => void;
  removeToast: (id: string) => void;
  error: (title: string, description?: string) => void;
  success: (title: string, description?: string) => void;
  info: (title: string, description?: string) => void;
}

export const useToastStore = create<ToastState>((set, get) => ({
  toasts: [],
  addToast: (toast) =>
    set((state) => ({
      toasts: [...state.toasts, { ...toast, id: crypto.randomUUID() }],
    })),
  removeToast: (id) =>
    set((state) => {
      const next = state.toasts.filter((t) => t.id !== id);
      return next.length === state.toasts.length ? state : { toasts: next };
    }),
  error: (title, description) =>
    get().addToast({ variant: "error", title, description }),
  success: (title, description) =>
    get().addToast({ variant: "success", title, description }),
  info: (title, description) =>
    get().addToast({ variant: "info", title, description }),
}));
