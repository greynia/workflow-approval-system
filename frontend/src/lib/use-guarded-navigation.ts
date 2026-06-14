import { useMemo } from "react";
import type React from "react";
import { useRouter } from "@/i18n/navigation";
import {
  useUnsavedChangesStore,
  type PendingAction,
} from "@/stores/unsaved-changes-store";

type Router = ReturnType<typeof useRouter>;

interface GuardedNavigation {
  /** Replace the current entry, prompting if the form is dirty. */
  replace: Router["replace"];
  /** Push a new entry, prompting if the form is dirty. */
  push: Router["push"];
  /** Run an arbitrary side-effect (e.g. signOut) behind the same prompt. */
  runGuarded: (action: PendingAction) => void;
  /**
   * Click handler for `<Link>` elements. Skips modifier-key clicks (which
   * open new tabs and don't navigate the current view) so the guard only
   * fires for plain in-tab navigation.
   */
  interceptLinkClick: (href: string, event: React.MouseEvent) => void;
}

export function useGuardedNavigation(): GuardedNavigation {
  const router = useRouter();

  return useMemo<GuardedNavigation>(() => {
    const isDirty = () => useUnsavedChangesStore.getState().dirty;
    const queue = (action: PendingAction) =>
      useUnsavedChangesStore.getState().request(action);

    return {
      replace: ((...args) => {
        if (isDirty()) queue(() => router.replace(...args));
        else router.replace(...args);
      }) as Router["replace"],
      push: ((...args) => {
        if (isDirty()) queue(() => router.push(...args));
        else router.push(...args);
      }) as Router["push"],
      runGuarded: (action) => {
        if (isDirty()) queue(action);
        else action();
      },
      interceptLinkClick: (href, event) => {
        if (event.defaultPrevented) return;
        if (
          event.metaKey ||
          event.ctrlKey ||
          event.shiftKey ||
          event.altKey ||
          event.button !== 0
        ) {
          return;
        }
        if (!isDirty()) return;
        event.preventDefault();
        queue(() => router.push(href));
      },
    };
  }, [router]);
}
