import { delay, http, HttpResponse } from "msw";
import {
  computeUsedMinutesByType,
  createMockLeaveRequest,
  findRequestById,
  hasOverlappingActiveLeave,
  listRequestsByApplicant,
  toLeaveRequestSummary,
} from "@/mocks/data/requests";
import { findMockEmployeeById } from "@/mocks/data/employees";
import { HEADER_MOCK_EMPLOYEE_ID } from "@/constants/app.constant";
import type { PageResponse } from "@/types/common";
import type {
  CreateLeaveRequest,
  LeaveBalanceResponse,
  LeaveRequestDetail,
  LeaveRequestSummary,
} from "@/types/leave";
import { mockErrorBody } from "./utils";

const MOCK_QUOTAS: Record<string, number> = {
  ANNUAL: 7200,   // 15 days × 480 min
  SICK: 14400,    // 30 days × 480 min
  PERSONAL: 3360, // 7 days × 480 min
};

export const requestHandlers = [
  http.get("/api/requests", async ({ request }) => {
    await delay(200);

    const employeeId = Number(request.headers.get(HEADER_MOCK_EMPLOYEE_ID));

    if (!Number.isInteger(employeeId) || employeeId <= 0) {
      return HttpResponse.json(
        mockErrorBody("UNAUTHORIZED", "Mock session is missing"),
        { status: 401 }
      );
    }

    const url = new URL(request.url);
    const page = Number(url.searchParams.get("page") ?? "0");
    const size = Number(url.searchParams.get("size") ?? "10");
    const requests = listRequestsByApplicant(employeeId);
    const start = page * size;
    const pagedItems = requests.slice(start, start + size).map(toLeaveRequestSummary);

    const response: PageResponse<LeaveRequestSummary> = {
      items: pagedItems,
      currentPage: page,
      totalCount: requests.length,
      pageSize: size,
      totalPages: Math.max(1, Math.ceil(requests.length / size)),
    };

    return HttpResponse.json(response, { status: 200 });
  }),

  http.post("/api/requests", async ({ request }) => {
    await delay(250);

    const applicantId = Number(request.headers.get(HEADER_MOCK_EMPLOYEE_ID));

    if (!Number.isInteger(applicantId) || applicantId <= 0) {
      return HttpResponse.json(
        mockErrorBody("UNAUTHORIZED", "Mock session is missing"),
        { status: 401 }
      );
    }

    const applicant = findMockEmployeeById(applicantId);

    if (!applicant) {
      return HttpResponse.json(
        mockErrorBody("RESOURCE_NOT_FOUND", "Employee not found"),
        { status: 404 }
      );
    }

    const approver =
      applicant.managerId == null ? null : findMockEmployeeById(applicant.managerId);

    if (!approver) {
      return HttpResponse.json(
        mockErrorBody("BUSINESS_RULE_ERROR", "Applicant manager is missing"),
        { status: 400 }
      );
    }

    const payload = (await request.json()) as CreateLeaveRequest;
    const deputy = findMockEmployeeById(payload.deputyId);

    if (!deputy) {
      return HttpResponse.json(
        mockErrorBody("VALIDATION_ERROR", "Deputy is invalid"),
        { status: 400 }
      );
    }

    if (payload.deputyId === applicantId) {
      return HttpResponse.json(
        mockErrorBody("VALIDATION_ERROR", "Cannot select yourself as deputy"),
        { status: 400 }
      );
    }

    if (hasOverlappingActiveLeave(applicantId, payload.startTime, payload.endTime)) {
      return HttpResponse.json(
        mockErrorBody("APPLICANT_ON_LEAVE", "Applicant has overlapping leave"),
        { status: 400 }
      );
    }

    if (hasOverlappingActiveLeave(payload.deputyId, payload.startTime, payload.endTime)) {
      return HttpResponse.json(
        mockErrorBody("DEPUTY_ON_LEAVE", "Deputy has overlapping leave"),
        { status: 400 }
      );
    }

    const created = createMockLeaveRequest({
      applicantId: applicant.id,
      applicantName: applicant.name,
      managerId: approver.id,
      managerName: approver.name,
      deputyId: deputy.id,
      deputyName: deputy.name,
      data: payload,
    });

    return HttpResponse.json(created, { status: 201 });
  }),

  http.get("/api/requests/pending/count", async ({ request }) => {
    await delay(100);

    const employeeId = Number(request.headers.get(HEADER_MOCK_EMPLOYEE_ID));

    if (!Number.isInteger(employeeId) || employeeId <= 0) {
      return HttpResponse.json(
        mockErrorBody("UNAUTHORIZED", "Mock session is missing"),
        { status: 401 }
      );
    }

    const count = listRequestsByApplicant(employeeId).filter(
      (r) => r.status === "PENDING"
    ).length;

    return HttpResponse.json({ count }, { status: 200 });
  }),

  http.get("/api/requests/balance", async ({ request }) => {
    await delay(150);

    const employeeId = Number(request.headers.get(HEADER_MOCK_EMPLOYEE_ID));

    if (!Number.isInteger(employeeId) || employeeId <= 0) {
      return HttpResponse.json(
        mockErrorBody("UNAUTHORIZED", "Mock session is missing"),
        { status: 401 }
      );
    }

    const balances: LeaveBalanceResponse[] = (["ANNUAL", "SICK", "PERSONAL"] as const).map(
      (leaveType) => {
        const quota = MOCK_QUOTAS[leaveType];
        const used = computeUsedMinutesByType(employeeId, leaveType);
        return { leaveType, quotaMinutes: quota, usedMinutes: used, remainingMinutes: quota - used };
      }
    );

    return HttpResponse.json(balances, { status: 200 });
  }),

  http.get("/api/requests/:id", async ({ params, request }) => {
    await delay(150);

    const employeeId = Number(request.headers.get(HEADER_MOCK_EMPLOYEE_ID));

    if (!Number.isInteger(employeeId) || employeeId <= 0) {
      return HttpResponse.json(
        mockErrorBody("UNAUTHORIZED", "Mock session is missing"),
        { status: 401 }
      );
    }

    const requestId = Number(params.id);
    const leaveRequest = findRequestById(requestId);

    if (!leaveRequest || leaveRequest.applicantId !== employeeId) {
      return HttpResponse.json(
        mockErrorBody("RESOURCE_NOT_FOUND", "Leave request not found"),
        { status: 404 }
      );
    }

    return HttpResponse.json<LeaveRequestDetail>(leaveRequest, { status: 200 });
  }),

  http.post("/api/requests/calculate", async ({ request }) => {
    await delay(150);

    const payload = (await request.json()) as { startTime: string; endTime: string };
    const durationMinutes = Math.max(
      30,
      Math.round((new Date(payload.endTime).getTime() - new Date(payload.startTime).getTime()) / 60000),
    );

    return HttpResponse.json({ durationMinutes }, { status: 200 });
  }),

  http.patch("/api/requests/:id/cancel", async () => new HttpResponse(null, { status: 204 })),
];
