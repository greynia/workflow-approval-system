import { delay, http, HttpResponse } from "msw";
import {
  findMockEmployeeById,
  mockEmployeeAccounts,
  toEmployeeSummary,
} from "@/mocks/data/employees";
import { hasOverlappingActiveLeave } from "@/mocks/data/requests";
import { HEADER_MOCK_EMPLOYEE_ID } from "@/constants/app.constant";
import { mockErrorBody } from "./utils";

function matchesQuery(value: string, query: string): boolean {
  return value.toLowerCase().includes(query.toLowerCase());
}

export const employeeHandlers = [
  http.get("/api/employees", async ({ request }) => {
    await delay(150);

    const currentEmployeeId = Number(
      request.headers.get(HEADER_MOCK_EMPLOYEE_ID)
    );

    if (!Number.isInteger(currentEmployeeId) || currentEmployeeId <= 0) {
      return HttpResponse.json(
        mockErrorBody("UNAUTHORIZED", "Mock session is missing"),
        { status: 401 }
      );
    }

    const currentEmployee = findMockEmployeeById(currentEmployeeId);

    if (!currentEmployee) {
      return HttpResponse.json(
        mockErrorBody("RESOURCE_NOT_FOUND", "Employee not found"),
        { status: 404 }
      );
    }

    const query = new URL(request.url).searchParams.get("query")?.trim();
    const employees = mockEmployeeAccounts
      .filter((employee) => employee.id !== currentEmployee.id)
      .filter((employee) =>
        !query
          ? true
          : matchesQuery(employee.name, query) || matchesQuery(employee.employeeNo, query)
      )
      .map(toEmployeeSummary);

    return HttpResponse.json(employees, { status: 200 });
  }),

  http.get("/api/employees/available-deputies", async ({ request }) => {
    await delay(150);

    const currentEmployeeId = Number(
      request.headers.get(HEADER_MOCK_EMPLOYEE_ID)
    );

    if (!Number.isInteger(currentEmployeeId) || currentEmployeeId <= 0) {
      return HttpResponse.json(
        mockErrorBody("UNAUTHORIZED", "Mock session is missing"),
        { status: 401 }
      );
    }

    const currentEmployee = findMockEmployeeById(currentEmployeeId);

    if (!currentEmployee) {
      return HttpResponse.json(
        mockErrorBody("RESOURCE_NOT_FOUND", "Employee not found"),
        { status: 404 }
      );
    }

    const url = new URL(request.url);
    const startTime = url.searchParams.get("startTime");
    const endTime = url.searchParams.get("endTime");
    const query = url.searchParams.get("query")?.trim();

    if (!startTime || !endTime) {
      return HttpResponse.json(
        mockErrorBody("VALIDATION_ERROR", "startTime and endTime are required"),
        { status: 400 }
      );
    }

    if (endTime <= startTime) {
      return HttpResponse.json(
        mockErrorBody("BAD_REQUEST", "endTime must be after startTime"),
        { status: 400 }
      );
    }

    const employees = mockEmployeeAccounts
      .filter((employee) => employee.id !== currentEmployee.id)
      .filter((employee) =>
        !query
          ? true
          : matchesQuery(employee.name, query) || matchesQuery(employee.employeeNo, query)
      )
      .filter((employee) => !hasOverlappingActiveLeave(employee.id, startTime, endTime))
      .map(toEmployeeSummary);

    return HttpResponse.json(employees, { status: 200 });
  }),
];
