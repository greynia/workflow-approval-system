import { screen, waitFor, fireEvent } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { server } from "@/mocks/server";
import { useAuthStore } from "@/stores/auth-store";
import { useToastStore } from "@/stores/toast-store";
import RequestsNewPage from "@/app/[locale]/(dashboard)/requests/new/page";
import { renderWithProviders } from "./test-utils";
import {
  mockEmployeeAccounts,
  toEmployeeSummary,
} from "@/mocks/data/employees";
import type { LeaveBalanceResponse } from "@/types/leave";

// Bypass the 500ms debounce so canCalculate becomes true immediately after
// dates are set — without this, the deputies query never fires in jsdom.
jest.mock("@/lib/use-debounce", () => ({
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  useDebounce: (value: any) => value,
}));

jest.mock("@/i18n/navigation", () => ({
  Link: ({ href, children, ...rest }: { href: string; children: React.ReactNode; [k: string]: unknown }) => (
    <a href={href} {...rest}>{children}</a>
  ),
  usePathname: () => "/requests/new",
  useRouter: () => ({ replace: jest.fn(), push: jest.fn() }),
}));

// Alice Chen is employee id=3; exclude herself from available deputies
const CURRENT_EMPLOYEE_ID = 3;
const availableDeputies = mockEmployeeAccounts
  .filter((e) => e.id !== CURRENT_EMPLOYEE_ID)
  .map(toEmployeeSummary);
const mockBalances: LeaveBalanceResponse[] = [
  { leaveType: "ANNUAL", quotaMinutes: 7200, usedMinutes: 0, remainingMinutes: 7200 },
  { leaveType: "SICK", quotaMinutes: 14400, usedMinutes: 0, remainingMinutes: 14400 },
  { leaveType: "PERSONAL", quotaMinutes: 3360, usedMinutes: 0, remainingMinutes: 3360 },
];
beforeEach(() => {
  useAuthStore.setState({
    user: { employeeId: CURRENT_EMPLOYEE_ID, name: "Alice Chen", role: "EMPLOYEE", permissions: ["request.view", "request.create", "request.edit", "employee.view", "balance.view"] },
  });
  useToastStore.setState({ toasts: [] });
  // Override only the auth-gated endpoint: default handler requires HEADER_MOCK_EMPLOYEE_ID
  // which isn't sent in Jest (NEXT_PUBLIC_ENABLE_MSW is not set to "true").
  // POST /api/requests/calculate does NOT require auth — the default handler works as-is.
  server.use(
    http.get("/api/employees/available-deputies", () =>
      HttpResponse.json(availableDeputies, { status: 200 })
    ),
    http.get("/api/requests/balance", () =>
      HttpResponse.json(mockBalances, { status: 200 })
    )
  );
});

describe("RequestsNewPage — form validation", () => {
  it("renders all required form field labels", async () => {
    renderWithProviders(<RequestsNewPage />);
    expect(screen.getByText("New Leave Request")).toBeInTheDocument();
    expect(screen.getByText("Leave Type")).toBeInTheDocument();
    expect(screen.getByText("Start Date")).toBeInTheDocument();
    expect(screen.getByText("End Date")).toBeInTheDocument();
    expect(screen.getByText("Deputy")).toBeInTheDocument();
  });

  it("shows validation errors on empty form submission", async () => {
    const { container } = renderWithProviders(<RequestsNewPage />);

    // The submit button is disabled until canSubmit is true.
    // fireEvent.submit bypasses the disabled state and triggers react-hook-form validation.
    const form = container.querySelector("form")!;
    fireEvent.submit(form);

    await waitFor(() => {
      expect(screen.getByText("Please select a leave type")).toBeInTheDocument();
    });
    expect(screen.getByText("Please select a start date")).toBeInTheDocument();
    expect(screen.getByText("Please select an end date")).toBeInTheDocument();
  });

  it("does not show errors before first submission attempt", () => {
    renderWithProviders(<RequestsNewPage />);
    expect(screen.queryByText("Please select a leave type")).not.toBeInTheDocument();
    expect(screen.queryByText("Please select a start date")).not.toBeInTheDocument();
  });
});
