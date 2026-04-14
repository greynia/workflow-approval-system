import { delay, http, HttpResponse } from "msw";
import { listPendingStepsByApprover } from "@/mocks/data/requests";
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
        stepOrder: step.stepOrder,
        requestId: req.id,
        applicantId: req.applicantId,
        applicantName: req.applicantName,
        leaveType: req.type,
        days: req.days,
        startDate: req.startDate,
        endDate: req.endDate,
        status: step.status,
        createdAt: step.createdAt,
      })
    );

    return HttpResponse.json(items, { status: 200 });
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
