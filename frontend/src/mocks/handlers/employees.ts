import { delay, http, HttpResponse } from "msw";
import {
  findMockEmployeeById,
  mockEmployeeAccounts,
  toEmployeeSummary,
} from "@/mocks/data/employees";
import { HEADER_MOCK_EMPLOYEE_ID } from "@/constants/app.constant";
import { mockErrorBody } from "./utils";

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

    const employees = mockEmployeeAccounts
      .filter((employee) => employee.id !== currentEmployee.id)
      .map(toEmployeeSummary);

    return HttpResponse.json(employees, { status: 200 });
  }),
];
