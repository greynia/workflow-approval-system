import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import AuditLogsPage from "@/app/[locale]/(dashboard)/admin/audit-logs/page";
import { renderWithProviders } from "./test-utils";
import { server } from "@/mocks/server";

jest.mock("@/i18n/navigation", () => ({
  Link: ({ href, children, ...rest }: { href: string; children: React.ReactNode; [k: string]: unknown }) => (
    <a href={href} {...rest}>{children}</a>
  ),
  usePathname: () => "/admin/audit-logs",
  useRouter: () => ({ replace: jest.fn(), push: jest.fn() }),
}));

describe("AuditLogsPage", () => {
  it("sends actorId filter in the request params", async () => {
    let capturedUrl: URL | null = null;

    server.use(
      http.get("/api/admin/audit-logs", ({ request }) => {
        capturedUrl = new URL(request.url);
        return HttpResponse.json({
          items: [],
          currentPage: 0,
          totalCount: 0,
          pageSize: 20,
          totalPages: 0,
        });
      }),
    );

    const user = userEvent.setup();
    renderWithProviders(<AuditLogsPage />);

    await waitFor(() => expect(capturedUrl).not.toBeNull());
    capturedUrl = null;

    await user.type(screen.getByPlaceholderText("Search by name..."), "Alice");

    await waitFor(() =>
      expect(capturedUrl?.searchParams.get("actorName")).toBe("Alice")
    );
  });
});
