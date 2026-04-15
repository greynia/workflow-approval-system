import { delay, http, HttpResponse } from "msw";
import { approveStep, listPendingStepsByApprover, rejectStep } from "@/mocks/data/requests";
import { findMockEmployeeById } from "@/mocks/data/employees";
import { HEADER_MOCK_EMPLOYEE_ID } from "@/constants/app.constant";
import type { PendingApproval, PendingApprovalCountResponse } from "@/types/approval";
import { mockErrorBody } from "./utils";

export const approvalHandlers = [
  http.get("/api/approvals/pending", async ({ request }) => {
    await delay(150);

    const employeeId = Number(request.headers.get(HEADER_MOCK_EMPLOYEE_ID));

    if (!Number.isInteger(employeeId) || employeeId <= 0) {
      return HttpResponse.json(
        mockErrorBody("UNAUTHORIZED", "Mock session is missing"),
        { status: 401 }
      );
    }

    const items: PendingApproval[] = listPendingStepsByApprover(employeeId).map(
      ({ step, request: req }) => ({
        stepId: step.id,
        stepType: step.stepType,
        requestId: req.id,
        applicantId: req.applicantId,
        applicantName: req.applicantName,
        leaveType: req.type,
        durationMinutes: req.durationMinutes,
        startTime: req.startTime,
        endTime: req.endTime,
        status: step.status,
        createdAt: step.createdAt,
      })
    );

    return HttpResponse.json(items, { status: 200 });
  }),

  http.post("/api/approvals/:stepId/approve", async ({ request, params }) => {
    await delay(200);

    const employeeId = Number(request.headers.get(HEADER_MOCK_EMPLOYEE_ID));
    if (!Number.isInteger(employeeId) || employeeId <= 0) {
      return HttpResponse.json(mockErrorBody("UNAUTHORIZED", "Mock session is missing"), { status: 401 });
    }

    const employee = findMockEmployeeById(employeeId);
    if (!employee) {
      return HttpResponse.json(mockErrorBody("UNAUTHORIZED", "Employee not found"), { status: 401 });
    }

    const stepId = Number(params.stepId);
    const body = await request.json().catch(() => ({})) as { comment?: string };
    const result = approveStep(stepId, employee.id, employee.name, body.comment);

    if (result.notFound) {
      return HttpResponse.json(mockErrorBody("RESOURCE_NOT_FOUND", "Step not found"), { status: 404 });
    }
    if (result.alreadyProcessed) {
      return HttpResponse.json(mockErrorBody("BAD_REQUEST", "Step already processed"), { status: 400 });
    }

    return new HttpResponse(null, { status: 204 });
  }),

  http.post("/api/approvals/:stepId/reject", async ({ request, params }) => {
    await delay(200);

    const employeeId = Number(request.headers.get(HEADER_MOCK_EMPLOYEE_ID));
    if (!Number.isInteger(employeeId) || employeeId <= 0) {
      return HttpResponse.json(mockErrorBody("UNAUTHORIZED", "Mock session is missing"), { status: 401 });
    }

    const employee = findMockEmployeeById(employeeId);
    if (!employee) {
      return HttpResponse.json(mockErrorBody("UNAUTHORIZED", "Employee not found"), { status: 401 });
    }

    const stepId = Number(params.stepId);
    const body = await request.json() as { comment: string };
    const result = rejectStep(stepId, employee.id, employee.name, body.comment);

    if (result.notFound) {
      return HttpResponse.json(mockErrorBody("RESOURCE_NOT_FOUND", "Step not found"), { status: 404 });
    }
    if (result.alreadyProcessed) {
      return HttpResponse.json(mockErrorBody("BAD_REQUEST", "Step already processed"), { status: 400 });
    }

    return new HttpResponse(null, { status: 204 });
  }),

  http.get("/api/approvals/pending/count", async ({ request }) => {
    await delay(100);

    const employeeId = Number(request.headers.get(HEADER_MOCK_EMPLOYEE_ID));

    if (!Number.isInteger(employeeId) || employeeId <= 0) {
      return HttpResponse.json(
        mockErrorBody("UNAUTHORIZED", "Mock session is missing"),
        { status: 401 }
      );
    }

    const count = listPendingStepsByApprover(employeeId).length;
    return HttpResponse.json<PendingApprovalCountResponse>({ count }, { status: 200 });
  }),
];
