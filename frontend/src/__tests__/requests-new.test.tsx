import { screen, waitFor, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { server } from "@/mocks/server";
import { useAuthStore } from "@/stores/auth-store";
import RequestsNewPage from "@/app/[locale]/(dashboard)/requests/new/page";
import { renderWithProviders } from "./test-utils";
import {
  mockEmployeeAccounts,
  toEmployeeSummary,
} from "@/mocks/data/employees";

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
beforeEach(() => {
  useAuthStore.setState({
    user: { employeeId: CURRENT_EMPLOYEE_ID, name: "Alice Chen", role: "EMPLOYEE" },
  });
  // Override only the auth-gated endpoint: default handler requires HEADER_MOCK_EMPLOYEE_ID
  // which isn't sent in Jest (NEXT_PUBLIC_ENABLE_MSW is not set to "true").
  // POST /api/requests/calculate does NOT require auth — the default handler works as-is.
  server.use(
    http.get("/api/employees/available-deputies", () =>
      HttpResponse.json(availableDeputies, { status: 200 })
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

describe("RequestsNewPage — submission", () => {
  /**
   * Sets up a fully-valid form state:
   * 1. Select leave type
   * 2. Fire change on both date inputs (triggers setValue on the hidden RHF fields)
   * 3. Wait for calculate query + deputies query to resolve
   * 4. Search and select deputy
   *
   * Select order: [0]=leave type, [1]=start time picker, [2]=end time picker
   */
  async function fillValidForm(container: HTMLElement) {
    const user = userEvent.setup();

    // Leave type — first select in the form
    const typeSelect = container.querySelectorAll("select")[0];
    await user.selectOptions(typeSelect, "SICK");

    // Date inputs call setValue("startTime"/"endTime") via their onChange handlers.
    // Use different days so endTime > startTime (required by schema).
    const dateInputs = container.querySelectorAll("input[type='date']");
    fireEvent.change(dateInputs[0], { target: { value: "2026-05-01" } });
    fireEvent.change(dateInputs[1], { target: { value: "2026-05-02" } });

    // Wait for deputies to load
    await waitFor(() =>
      expect(screen.queryByText("Loading available deputies...")).not.toBeInTheDocument()
    );

    const deputyInput = screen.getByPlaceholderText("Search by name or employee no.");
    await user.click(deputyInput);
    await user.type(deputyInput, "Bob");
    const allSelects = container.querySelectorAll("select");
    await user.selectOptions(allSelects[3], "4");

    // Wait for the calculate query to resolve — the submit button is disabled until
    // durationMinutes >= 30 (set via useEffect after calculate returns).
    await waitFor(() =>
      expect(container.querySelector("button[type='submit']")).not.toBeDisabled()
    , { timeout: 3000 });
  }

  it("shows success toast after valid submit", async () => {
    const user = userEvent.setup();
    server.use(
      http.post("/api/requests", () =>
        HttpResponse.json({ id: 9999 }, { status: 201 })
      )
    );

    const { container } = renderWithProviders(<RequestsNewPage />);
    await fillValidForm(container);

    await user.click(screen.getByRole("button", { name: "Submit" }));

    await waitFor(() => {
      expect(screen.getByText("Leave request submitted")).toBeInTheDocument();
    }, { timeout: 3000 });
  });

  it("shows error toast when API returns 400", async () => {
    const user = userEvent.setup();
    server.use(
      http.post("/api/requests", () =>
        HttpResponse.json(
          { code: "BAD_REQUEST", message: "BAD_REQUEST", traceId: "x" },
          { status: 400 }
        )
      )
    );

    const { container } = renderWithProviders(<RequestsNewPage />);
    await fillValidForm(container);

    await user.click(screen.getByRole("button", { name: "Submit" }));

    await waitFor(() => {
      expect(
        screen.getByText("Validation failed. Please check your input.")
      ).toBeInTheDocument();
    }, { timeout: 3000 });
  });
});
