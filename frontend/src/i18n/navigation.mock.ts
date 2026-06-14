import NextLink from "next/link"

export const Link = NextLink

export const usePathname = () => "/"

export const useRouter = () => ({
  push: (_href: string) => {},
  replace: (_href: string) => {},
  back: () => {},
  forward: () => {},
  refresh: () => {},
  prefetch: (_href: string) => {},
})

export const redirect = (_href: string) => {}

export const getPathname = (_args: unknown) => "/"
