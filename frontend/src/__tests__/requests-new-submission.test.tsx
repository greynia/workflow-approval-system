import { fireEvent, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { server } from "@/mocks/server";
import { useAuthStore } from "@/stores/auth-store";
import { useToastStore } from "@/stores/toast-store";
import { renderWithProviders } from "./test-utils";
import {
  mockEmployeeAccounts,
  toEmployeeSummary,
} from "@/mocks/data/employees";
import type { LeaveBalanceResponse } from "@/types/leave";

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

const validFormValues = {
  type: "SICK",
  startTime: "2026-05-01T09:00",
  endTime: "2026-05-02T09:00",
  durationMinutes: 480,
  deputyId: 4,
  reason: "",
};

jest.mock("react-hook-form", () => ({
  useForm: () => ({
    register: (name: string) => ({ name }),
    handleSubmit:
      (onSubmit: (values: typeof validFormValues) => void) =>
      (event?: { preventDefault?: () => void }) => {
        event?.preventDefault?.();
        return onSubmit(validFormValues);
      },
    setValue: jest.fn(),
    control: {},
    formState: { errors: {}, isSubmitting: false },
  }),
  useWatch: ({ name }: { name?: keyof typeof validFormValues }) =>
    name ? validFormValues[name] : undefined,
}));

import RequestsNewPage from "@/app/[locale]/(dashboard)/requests/new/page";

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
  server.use(
    http.get("/api/employees/available-deputies", () =>
      HttpResponse.json(availableDeputies, { status: 200 })
    ),
    http.get("/api/requests/balance", () =>
      HttpResponse.json(mockBalances, { status: 200 })
    )
  );
});

describe("RequestsNewPage — submission side effects", () => {
  it("shows success toast after successful submit", async () => {
    const createRequestSpy = jest.fn();
    server.use(
      http.post("/api/requests", () => {
        createRequestSpy();
        return HttpResponse.json({ id: 9999 }, { status: 201 });
      })
    );

    const { container } = renderWithProviders(<RequestsNewPage />);
    fireEvent.submit(container.querySelector("form")!);

    await waitFor(() => {
      expect(createRequestSpy).toHaveBeenCalled();
      expect(useToastStore.getState().toasts).toEqual(
        expect.arrayContaining([
          expect.objectContaining({ variant: "success", title: "Leave request submitted" }),
        ])
      );
    });
  });

  it("shows error toast when API returns 400", async () => {
    const createRequestSpy = jest.fn();
    server.use(
      http.post("/api/requests", () => {
        createRequestSpy();
        return HttpResponse.json(
          { code: "BAD_REQUEST", message: "BAD_REQUEST", requestId: "x" },
          { status: 400 }
        );
      })
    );

    const { container } = renderWithProviders(<RequestsNewPage />);
    fireEvent.submit(container.querySelector("form")!);

    await waitFor(() => {
      expect(createRequestSpy).toHaveBeenCalled();
      expect(useToastStore.getState().toasts).toEqual(
        expect.arrayContaining([
          expect.objectContaining({
            variant: "error",
            title: "Validation failed. Please check your input.",
          }),
        ])
      );
    });
  });

  it("shows backend error message when API returns a readable message", async () => {
    const createRequestSpy = jest.fn();
    server.use(
      http.post("/api/requests", () => {
        createRequestSpy();
        return HttpResponse.json(
          {
            code: "BAD_REQUEST",
            message: "申請人在所選時段已有請假紀錄",
            requestId: "02e2dde3-bec3-41c3-bbb5-0c11d5813189",
          },
          { status: 400 }
        );
      })
    );

    const { container } = renderWithProviders(<RequestsNewPage />);
    fireEvent.submit(container.querySelector("form")!);

    await waitFor(() => {
      expect(createRequestSpy).toHaveBeenCalled();
      expect(useToastStore.getState().toasts).toEqual(
        expect.arrayContaining([
          expect.objectContaining({
            variant: "error",
            title: "申請人在所選時段已有請假紀錄",
          }),
        ])
      );
    });
  });
});
