export function useAuthority(
  userAuthority: string[] = [],
  authority: string[] = [],
): boolean {
  if (!authority.length) return true;
  if (!userAuthority.length) return false;
  return authority.some((p) => userAuthority.includes(p));
}
