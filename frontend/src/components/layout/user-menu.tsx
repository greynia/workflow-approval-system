"use client";

import * as DropdownMenu from "@radix-ui/react-dropdown-menu";
import { useTranslations } from "next-intl";
import { useAuth } from "@/hooks/useAuth";
import { useAuthStore } from "@/stores/auth-store";
import { Skeleton } from "@/components/ui/skeleton";
import { useGuardedNavigation } from "@/lib/use-guarded-navigation";

/**
 * Header dropdown showing the signed-in user's display name and role,
 * with a sign-out action.
 *
 * Renders a skeleton placeholder while `useAuthStore.user` is `null`.
 * Reads translations from the `Header` namespace (e.g. `Header.Role.<role>`, `Header.SignOut`).
 * No props — relies on the auth store and the surrounding `next-intl` provider.
 */
export function UserMenu() {
  const t = useTranslations("Header");
  const user = useAuthStore((s) => s.user);
  const { signOut } = useAuth();
  const guardedNav = useGuardedNavigation();

  if (!user) {
    return (
      <div className="flex items-center gap-2">
        <Skeleton className="h-8 w-24" />
      </div>
    );
  }

  return (
    <DropdownMenu.Root>
      <DropdownMenu.Trigger asChild>
        <button
          type="button"
          className="flex items-center gap-2 rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium text-zinc-700 transition-colors hover:bg-zinc-100"
        >
          <span>{user.name}</span>
          <span className="rounded bg-zinc-100 px-1.5 py-0.5 text-xs text-zinc-600">
            {t(`Role.${user.role}`)}
          </span>
        </button>
      </DropdownMenu.Trigger>

      <DropdownMenu.Portal>
        <DropdownMenu.Content
          align="end"
          sideOffset={4}
          className="z-50 min-w-[160px] rounded-md border border-zinc-200 bg-white p-1 shadow-md"
        >
          <DropdownMenu.Item
            onSelect={() => guardedNav.runGuarded(() => void signOut())}
            className="cursor-pointer rounded px-3 py-2 text-sm text-zinc-700 outline-none hover:bg-zinc-100 focus:bg-zinc-100"
          >
            {t("SignOut")}
          </DropdownMenu.Item>
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  );
}
