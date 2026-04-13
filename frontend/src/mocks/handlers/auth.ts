import { delay, http, HttpResponse } from "msw";
import {
  findMockEmployeeByCredentials,
  findMockEmployeeById,
  toEmployeeResponse,
  toLoginResponse,
} from "@/mocks/data/employees";
import { HEADER_MOCK_EMPLOYEE_ID } from "@/constants/app.constant";
import type { LoginRequest } from "@/types/auth";
import { mockErrorBody } from "./utils";

export const authHandlers = [
  http.post("/api/auth/login", async ({ request }) => {
    await delay(250);

    const credentials = (await request.json()) as LoginRequest;
    const employee = findMockEmployeeByCredentials(credentials);

    if (!employee) {
      return HttpResponse.json(
        mockErrorBody("INVALID_CREDENTIALS", "Invalid email or password"),
        { status: 401 }
      );
    }

    return HttpResponse.json(toLoginResponse(employee), { status: 200 });
  }),

  http.post("/api/auth/logout", async () => {
    await delay(100);
    return new HttpResponse(null, { status: 204 });
  }),

  http.get("/api/auth/me", async ({ request }) => {
    await delay(150);

    const employeeId = Number(request.headers.get(HEADER_MOCK_EMPLOYEE_ID));

    if (!Number.isInteger(employeeId) || employeeId <= 0) {
      return HttpResponse.json(
        mockErrorBody("UNAUTHORIZED", "Mock session is missing"),
        { status: 401 }
      );
    }

    const employee = findMockEmployeeById(employeeId);

    if (!employee) {
      return HttpResponse.json(
        mockErrorBody("RESOURCE_NOT_FOUND", "Employee not found"),
        { status: 404 }
      );
    }

    return HttpResponse.json(toEmployeeResponse(employee), { status: 200 });
  }),
];
