import { create } from "zustand";

export type PendingAction = () => void;

interface UnsavedChangesState {
  /** True while at least one mounted form reports unsaved changes. */
  dirty: boolean;
  /** Action queued behind the confirm dialog; null when no prompt is pending. */
  pending: PendingAction | null;
  /**
   * Updates the dirty flag. Called by `useUnsavedChangesGuard` from inside
   * form pages — never from navigation triggers.
   */
  setDirty: (value: boolean) => void;
  /**
   * Queues a navigation/action to run after the user confirms in the dialog.
   * Caller is responsible for not invoking the action itself.
   */
  request: (action: PendingAction) => void;
  /** User confirmed leave — runs the queued action and clears the dirty flag. */
  confirm: () => void;
  /** User chose to stay — discards the queued action without running it. */
  cancel: () => void;
}

export const useUnsavedChangesStore = create<UnsavedChangesState>((set, get) => ({
  dirty: false,
  pending: null,
  setDirty: (value) => set({ dirty: value }),
  request: (action) => set({ pending: action }),
  confirm: () => {
    const { pending } = get();
    set({ pending: null, dirty: false });
    pending?.();
  },
  cancel: () => set({ pending: null }),
}));
