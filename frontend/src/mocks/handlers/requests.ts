import { delay, http, HttpResponse } from "msw";
import {
  createMockLeaveRequest,
  findRequestById,
  listRequestsByApplicant,
  toLeaveRequestSummary,
} from "@/mocks/data/requests";
import { findMockEmployeeById } from "@/mocks/data/employees";
import { HEADER_MOCK_EMPLOYEE_ID } from "@/constants/app.constant";
import type { PageResponse } from "@/types/common";
import type {
  CreateLeaveRequest,
  LeaveRequestDetail,
  LeaveRequestSummary,
} from "@/types/leave";
import { mockErrorBody } from "./utils";

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
    const deputy =
      payload.deputyId == null ? null : findMockEmployeeById(payload.deputyId);

    if (payload.deputyId != null && !deputy) {
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

    const created = createMockLeaveRequest({
      applicantId: applicant.id,
      applicantName: applicant.name,
      approverId: approver.id,
      approverName: approver.name,
      deputyId: deputy?.id ?? null,
      deputyName: deputy?.name ?? null,
      data: payload,
    });

    return HttpResponse.json(created, { status: 201 });
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
];
