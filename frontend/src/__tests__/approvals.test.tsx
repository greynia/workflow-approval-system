import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { server } from "@/mocks/server";
import { useAuthStore } from "@/stores/auth-store";
import ApprovalsPage from "@/app/[locale]/(dashboard)/approvals/page";
import { renderWithProviders } from "./test-utils";
import type { PendingApproval } from "@/types/approval";

// Mock next-intl navigation — the real version requires Next.js router context
jest.mock("@/i18n/navigation", () => ({
  Link: ({ href, children, ...rest }: { href: string; children: React.ReactNode; [k: string]: unknown }) => (
    <a href={href} {...rest}>{children}</a>
  ),
  usePathname: () => "/approvals",
  useRouter: () => ({ replace: jest.fn(), push: jest.fn() }),
}));

// Matches the seed record in src/mocks/data/requests.ts (request 1001, step 5001).
// The default handler is auth-gated (HEADER_MOCK_EMPLOYEE_ID), so we override it here
// and type it against PendingApproval to catch shape drift at compile time.
const mockApprovals: PendingApproval[] = [
  {
    stepId: 5001,
    stepType: "DEPUTY",
    requestId: 1001,
    applicantId: 3,
    applicantName: "Alice Chen",
    leaveType: "ANNUAL",
    durationMinutes: 960,
    startTime: "2026-04-15T09:00",
    endTime: "2026-04-16T18:00",
    status: "PENDING",
    createdAt: "2026-04-10T09:00:00.000Z",
  },
];

beforeEach(() => {
  useAuthStore.setState({
    user: { employeeId: 2, name: "Mina Manager", role: "MANAGER", permissions: ["request.view", "request.create", "request.edit", "approval.view", "approval.approve", "employee.view", "balance.view"] },
  });
  // Override handler — no employee-ID check needed in unit tests
  server.use(
    http.get("/api/approvals/pending", () =>
      HttpResponse.json(mockApprovals, { status: 200 })
    ),
    http.get("/api/approvals/pending/count", () =>
      HttpResponse.json({ count: 1 }, { status: 200 })
    )
  );
});

describe("ApprovalsPage", () => {
  it("shows loading skeleton initially", () => {
    renderWithProviders(<ApprovalsPage />);
    // Skeletons are pulse divs — they render before the query resolves
    const skeletonCells = document.querySelectorAll(".animate-pulse");
    expect(skeletonCells.length).toBeGreaterThan(0);
  });

  it("renders approval list after data loads", async () => {
    renderWithProviders(<ApprovalsPage />);
    await waitFor(() => {
      // RWD renders both desktop table and mobile cards — both are in jsdom DOM
      expect(screen.getAllByText("Alice Chen").length).toBeGreaterThan(0);
    });
    expect(screen.getAllByText("Annual Leave").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Approve").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Reject").length).toBeGreaterThan(0);
  });

  it("shows empty state when no pending approvals", async () => {
    server.use(
      http.get("/api/approvals/pending", () =>
        HttpResponse.json([], { status: 200 })
      )
    );
    renderWithProviders(<ApprovalsPage />);
    await waitFor(() => {
      expect(screen.getByText("No pending approvals")).toBeInTheDocument();
    });
  });

  it("shows error state when API fails", async () => {
    server.use(
      http.get("/api/approvals/pending", () =>
        HttpResponse.json({}, { status: 500 })
      )
    );
    renderWithProviders(<ApprovalsPage />);
    await waitFor(() => {
      expect(
        screen.getByText("Failed to load pending approvals")
      ).toBeInTheDocument();
    });
  });

  describe("Approve dialog", () => {
    it("opens when Approve button is clicked", async () => {
      const user = userEvent.setup();
      renderWithProviders(<ApprovalsPage />);
      await waitFor(() => expect(screen.getAllByText("Approve").length).toBeGreaterThan(0));

      // Click the first Approve button (desktop table or mobile card)
      await user.click(screen.getAllByText("Approve")[0]);

      expect(screen.getByText("Confirm Approval")).toBeInTheDocument();
      expect(screen.getByText("Are you sure you want to approve this leave request?")).toBeInTheDocument();
    });

    it("calls approve API and closes dialog on confirm", async () => {
      let apiCalled = false;
      server.use(
        http.post("/api/approvals/:stepId/approve", () => {
          apiCalled = true;
          return new HttpResponse(null, { status: 204 });
        })
      );

      const user = userEvent.setup();
      renderWithProviders(<ApprovalsPage />);
      await waitFor(() =>
        expect(screen.getAllByText("Approve").length).toBeGreaterThan(0)
      );

      // Open dialog
      await user.click(screen.getAllByRole("button", { name: "Approve" })[0]);
      expect(screen.getByText("Confirm Approval")).toBeInTheDocument();

      // Confirm inside dialog — the dialog's confirm button is the only "Approve"
      // button with the dialog open, so getByRole is now unambiguous
      const confirmBtn = screen.getAllByRole("button", { name: "Approve" }).at(-1)!;
      await user.click(confirmBtn);

      // Dialog should close after successful API call
      await waitFor(() =>
        expect(screen.queryByText("Confirm Approval")).not.toBeInTheDocument()
      );
      expect(apiCalled).toBe(true);
    });

    it("invalidates cached request detail after approve", async () => {
      server.use(
        http.post("/api/approvals/:stepId/approve", () =>
          new HttpResponse(null, { status: 204 })
        )
      );

      const user = userEvent.setup();
      const { queryClient } = renderWithProviders(<ApprovalsPage />);
      const invalidateSpy = jest.spyOn(queryClient, "invalidateQueries");

      await waitFor(() =>
        expect(screen.getAllByRole("button", { name: "Approve" }).length).toBeGreaterThan(0)
      );

      await user.click(screen.getAllByRole("button", { name: "Approve" })[0]);
      const confirmBtn = screen.getAllByRole("button", { name: "Approve" }).at(-1)!;
      await user.click(confirmBtn);

      await waitFor(() =>
        expect(screen.queryByText("Confirm Approval")).not.toBeInTheDocument()
      );
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ["requests"] });
    });
  });

  describe("Reject dialog", () => {
    it("opens when Reject button is clicked", async () => {
      const user = userEvent.setup();
      renderWithProviders(<ApprovalsPage />);
      await waitFor(() => expect(screen.getAllByText("Reject").length).toBeGreaterThan(0));

      await user.click(screen.getAllByText("Reject")[0]);

      expect(screen.getByText("Reject Request")).toBeInTheDocument();
    });

    it("shows validation error when comment is empty", async () => {
      const user = userEvent.setup();
      renderWithProviders(<ApprovalsPage />);
      await waitFor(() => expect(screen.getAllByText("Reject").length).toBeGreaterThan(0));

      await user.click(screen.getAllByText("Reject")[0]);

      // The dialog's confirm button has text "Reject" inside bg-red-600 button
      const confirmBtn = screen.getByRole("button", { name: "Reject" });
      await user.click(confirmBtn);

      expect(
        screen.getByText("Please enter a rejection reason")
      ).toBeInTheDocument();
    });

    it("invalidates cached request detail after reject", async () => {
      server.use(
        http.post("/api/approvals/:stepId/reject", () =>
          new HttpResponse(null, { status: 204 })
        )
      );

      const user = userEvent.setup();
      const { queryClient } = renderWithProviders(<ApprovalsPage />);
      const invalidateSpy = jest.spyOn(queryClient, "invalidateQueries");

      await waitFor(() =>
        expect(screen.getAllByText("Reject").length).toBeGreaterThan(0)
      );

      await user.click(screen.getAllByText("Reject")[0]);
      await user.type(
        screen.getByPlaceholderText("Enter rejection reason (required)"),
        "Need more information",
      );
      await user.click(screen.getByRole("button", { name: "Reject" }));

      await waitFor(() =>
        expect(screen.queryByText("Reject Request")).not.toBeInTheDocument()
      );
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ["requests"] });
    });
  });
});
