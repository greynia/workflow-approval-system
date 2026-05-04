import { useEffect } from "react";
import { useUnsavedChangesStore } from "@/stores/unsaved-changes-store";

interface Options {
  /**
   * Whether the current form has unsaved changes worth warning about.
   * Typically `formState.isDirty && !formState.isSubmitSuccessful`.
   */
  when: boolean;
}

/**
 * Registers the current page as a source of unsaved changes.
 *
 * Writes directly to the shared store (no selector) so the hook never causes
 * the host component to re-render in response to its own writes — important
 * when the host is a form whose render cycle would otherwise feed back into
 * `when`.
 *
 * Coordinates with `useGuardedNavigation` (SPA-level interception) and the
 * browser's `beforeunload` event (reload / close tab / back to non-app URL).
 * Cleans up on unmount so the store never reports stale dirty state.
 */
export function useUnsavedChangesGuard({ when }: Options): void {
  useEffect(() => {
    useUnsavedChangesStore.setState({ dirty: when });
    return () => {
      useUnsavedChangesStore.setState({ dirty: false });
    };
  }, [when]);

  useEffect(() => {
    if (!when) return;
    function onBeforeUnload(event: BeforeUnloadEvent) {
      event.preventDefault();
    }
    window.addEventListener("beforeunload", onBeforeUnload);
    return () => window.removeEventListener("beforeunload", onBeforeUnload);
  }, [when]);
}
