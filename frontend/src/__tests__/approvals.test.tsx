import { act, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { server } from "@/mocks/server";
import { useAuthStore } from "@/stores/auth-store";
import { useUnsavedChangesStore } from "@/stores/unsaved-changes-store";
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
    stepType: "MANAGER",
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

const mockDeputyApprovals: PendingApproval[] = [
  {
    stepId: 5002,
    stepType: "DEPUTY",
    requestId: 1002,
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
  useUnsavedChangesStore.setState({ dirty: false, pending: null });
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

  describe("Deputy step — semantic UI", () => {
    beforeEach(() => {
      // Override base handler with deputy-step fixture for this group
      server.use(
        http.get("/api/approvals/pending", () =>
          HttpResponse.json(mockDeputyApprovals, { status: 200 })
        )
      );
    });

    it("renders Accept/Decline buttons and Deputy step badge", async () => {
      renderWithProviders(<ApprovalsPage />);
      await waitFor(() =>
        expect(screen.getAllByText("Accept").length).toBeGreaterThan(0)
      );
      expect(screen.getAllByText("Decline").length).toBeGreaterThan(0);
      expect(screen.getAllByText("Deputy Confirmation").length).toBeGreaterThan(0);
      // No manager-step labels should leak through for a deputy row
      expect(screen.queryByText("Approve")).not.toBeInTheDocument();
      expect(screen.queryByText("Reject")).not.toBeInTheDocument();
    });

    it("opens deputy-specific approve dialog", async () => {
      const user = userEvent.setup();
      renderWithProviders(<ApprovalsPage />);
      await waitFor(() =>
        expect(screen.getAllByText("Accept").length).toBeGreaterThan(0)
      );

      await user.click(screen.getAllByRole("button", { name: "Accept" })[0]);

      expect(screen.getByText("Accept Deputy Role")).toBeInTheDocument();
      expect(
        screen.getByText(
          "You will cover this colleague's work during their leave. The request then moves to manager review.",
        ),
      ).toBeInTheDocument();
    });

    it("opens deputy-specific reject dialog with deputy-flavored copy", async () => {
      const user = userEvent.setup();
      renderWithProviders(<ApprovalsPage />);
      await waitFor(() =>
        expect(screen.getAllByText("Decline").length).toBeGreaterThan(0)
      );

      await user.click(screen.getAllByRole("button", { name: "Decline" })[0]);

      expect(screen.getByText("Decline Deputy Role")).toBeInTheDocument();
      expect(
        screen.getByPlaceholderText("Explain why you cannot cover (required)"),
      ).toBeInTheDocument();
    });

    it("shows deputy-specific success toast on approve", async () => {
      server.use(
        http.post("/api/approvals/:stepId/approve", () =>
          new HttpResponse(null, { status: 204 })
        )
      );
      const user = userEvent.setup();
      renderWithProviders(<ApprovalsPage />);
      await waitFor(() =>
        expect(screen.getAllByText("Accept").length).toBeGreaterThan(0)
      );

      await user.click(screen.getAllByRole("button", { name: "Accept" })[0]);
      const confirmBtn = screen.getAllByRole("button", { name: "Accept" }).at(-1)!;
      await user.click(confirmBtn);

      await waitFor(() =>
        expect(screen.getByText("Deputy role accepted")).toBeInTheDocument()
      );
    });

    // Guards against the race where approveTarget is cleared (via ESC / backdrop)
    // before the API resolves. The deputy flag is carried in mutation variables,
    // so the success toast must still reflect the originating step type.
    it("preserves deputy-flavored success toast when dialog is dismissed mid-flight", async () => {
      let resolveApprove: () => void = () => {};
      server.use(
        http.post(
          "/api/approvals/:stepId/approve",
          async () =>
            await new Promise<HttpResponse<null>>((resolve) => {
              resolveApprove = () => resolve(new HttpResponse(null, { status: 204 }));
            }),
        ),
      );

      const user = userEvent.setup();
      renderWithProviders(<ApprovalsPage />);
      await waitFor(() =>
        expect(screen.getAllByText("Accept").length).toBeGreaterThan(0)
      );

      // Open deputy approve dialog and submit
      await user.click(screen.getAllByRole("button", { name: "Accept" })[0]);
      const confirmBtn = screen.getAllByRole("button", { name: "Accept" }).at(-1)!;
      await user.click(confirmBtn);

      // Dismiss the dialog while the mutation is still pending; this clears
      // approveTarget. A closure-based onSuccess would now read null and
      // fall back to the manager toast — variables snapshot must prevent that.
      await user.keyboard("{Escape}");

      act(() => {
        resolveApprove();
      });

      await waitFor(() =>
        expect(screen.getByText("Deputy role accepted")).toBeInTheDocument()
      );
    });
  });

  describe("Reject dialog — unsaved-changes guard", () => {
    it("closes immediately on Cancel when textarea is empty", async () => {
      const user = userEvent.setup();
      renderWithProviders(<ApprovalsPage />);
      await waitFor(() =>
        expect(screen.getAllByText("Reject").length).toBeGreaterThan(0)
      );

      await user.click(screen.getAllByText("Reject")[0]);
      expect(screen.getByText("Reject Request")).toBeInTheDocument();

      await user.click(screen.getByRole("button", { name: "Cancel" }));

      expect(screen.queryByText("Reject Request")).not.toBeInTheDocument();
      expect(useUnsavedChangesStore.getState().pending).toBeNull();
      expect(useUnsavedChangesStore.getState().dirty).toBe(false);
    });

    it("queues a pending action on Cancel when textarea has content", async () => {
      const user = userEvent.setup();
      renderWithProviders(<ApprovalsPage />);
      await waitFor(() =>
        expect(screen.getAllByText("Reject").length).toBeGreaterThan(0)
      );

      await user.click(screen.getAllByText("Reject")[0]);
      await user.type(
        screen.getByPlaceholderText("Enter rejection reason (required)"),
        "Need more information",
      );
      expect(useUnsavedChangesStore.getState().dirty).toBe(true);

      await user.click(screen.getByRole("button", { name: "Cancel" }));

      // Reject dialog still open; close action queued behind global guard
      expect(screen.getByText("Reject Request")).toBeInTheDocument();
      expect(useUnsavedChangesStore.getState().pending).not.toBeNull();
    });

    it("closes the dialog after the queued action is confirmed", async () => {
      const user = userEvent.setup();
      renderWithProviders(<ApprovalsPage />);
      await waitFor(() =>
        expect(screen.getAllByText("Reject").length).toBeGreaterThan(0)
      );

      await user.click(screen.getAllByText("Reject")[0]);
      await user.type(
        screen.getByPlaceholderText("Enter rejection reason (required)"),
        "Need more information",
      );
      await user.click(screen.getByRole("button", { name: "Cancel" }));

      // Simulate the user clicking "Leave" in the global UnsavedChangesDialog
      act(() => {
        useUnsavedChangesStore.getState().confirm();
      });

      await waitFor(() =>
        expect(screen.queryByText("Reject Request")).not.toBeInTheDocument()
      );
      expect(useUnsavedChangesStore.getState().pending).toBeNull();
      expect(useUnsavedChangesStore.getState().dirty).toBe(false);
    });

    it("preserves textarea content when the user chooses Stay", async () => {
      const user = userEvent.setup();
      renderWithProviders(<ApprovalsPage />);
      await waitFor(() =>
        expect(screen.getAllByText("Reject").length).toBeGreaterThan(0)
      );

      await user.click(screen.getAllByText("Reject")[0]);
      const textarea = screen.getByPlaceholderText(
        "Enter rejection reason (required)",
      ) as HTMLTextAreaElement;
      await user.type(textarea, "Need more information");
      await user.click(screen.getByRole("button", { name: "Cancel" }));

      // Simulate the user clicking "Stay" in the global UnsavedChangesDialog
      act(() => {
        useUnsavedChangesStore.getState().cancel();
      });

      expect(screen.getByText("Reject Request")).toBeInTheDocument();
      expect(textarea.value).toBe("Need more information");
      expect(useUnsavedChangesStore.getState().pending).toBeNull();
      // Still dirty — the user is still typing
      expect(useUnsavedChangesStore.getState().dirty).toBe(true);
    });
  });
});
