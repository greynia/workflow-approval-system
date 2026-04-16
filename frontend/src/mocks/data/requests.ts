import type {
  ApprovalStepResponse,
  CreateLeaveRequest,
  LeaveRequestDetail,
  LeaveRequestSummary,
} from "@/types/leave";

type MockLeaveRequestRecord = LeaveRequestDetail;

const initialMockLeaveRequests: MockLeaveRequestRecord[] = [
  {
    id: 1001,
    applicantId: 3,
    applicantName: "Alice Chen",
    deputyId: 4,
    deputyName: "Bob Wang",
    type: "ANNUAL",
    startTime: "2026-04-15T09:00",
    endTime: "2026-04-16T18:00",
    durationMinutes: 960,
    reason: "Family trip",
    status: "PENDING",
    currentStage: "WAITING_DEPUTY",
    createdAt: "2026-04-10T09:00:00.000Z",
    updatedAt: "2026-04-10T09:00:00.000Z",
    approvalSteps: [
      {
        id: 5001,
        approverId: 4,
        approverName: "Bob Wang",
        stepType: "DEPUTY",
        status: "PENDING",
        createdAt: "2026-04-10T09:00:00.000Z",
        updatedAt: "2026-04-10T09:00:00.000Z",
      },
      {
        id: 5002,
        approverId: 2,
        approverName: "Mina Manager",
        stepType: "MANAGER",
        status: "PENDING",
        createdAt: "2026-04-10T09:00:00.000Z",
        updatedAt: "2026-04-10T09:00:00.000Z",
      },
    ],
    approvalActions: [],
  },
];

let requestsDb: MockLeaveRequestRecord[] = structuredClone(initialMockLeaveRequests);
let requestSequence = 2000;
let approvalStepSequence = 6000;
let approvalActionSequence = 8000;

export function listRequestsByApplicant(applicantId: number): MockLeaveRequestRecord[] {
  return requestsDb
    .filter((request) => request.applicantId === applicantId)
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt));
}

export function findRequestById(id: number): MockLeaveRequestRecord | undefined {
  return requestsDb.find((request) => request.id === id);
}

export function hasOverlappingActiveLeave(
  applicantId: number,
  startTime: string,
  endTime: string,
): boolean {
  return requestsDb.some((request) =>
    request.applicantId === applicantId &&
    (request.status === "PENDING" || request.status === "APPROVED") &&
    request.startTime < endTime &&
    request.endTime > startTime
  );
}

export function createMockLeaveRequest(input: {
  applicantId: number;
  applicantName: string;
  managerId: number;
  managerName: string;
  deputyId: number;
  deputyName: string;
  data: CreateLeaveRequest;
}): MockLeaveRequestRecord {
  const now = new Date().toISOString();
  const durationMinutes = calculateDurationMinutes(input.data.startTime, input.data.endTime);
  const approvalSteps: ApprovalStepResponse[] = [
    {
      id: approvalStepSequence++,
      approverId: input.deputyId,
      approverName: input.deputyName,
      stepType: "DEPUTY",
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    },
    {
      id: approvalStepSequence++,
      approverId: input.managerId,
      approverName: input.managerName,
      stepType: "MANAGER",
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    },
  ];

  const request: MockLeaveRequestRecord = {
    id: requestSequence++,
    applicantId: input.applicantId,
    applicantName: input.applicantName,
    deputyId: input.deputyId,
    deputyName: input.deputyName,
    type: input.data.type,
    startTime: input.data.startTime,
    endTime: input.data.endTime,
    durationMinutes,
    reason: input.data.reason ?? null,
    status: "PENDING",
    currentStage: "WAITING_DEPUTY",
    createdAt: now,
    updatedAt: now,
    approvalSteps,
    approvalActions: [],
  };

  requestsDb = [request, ...requestsDb];
  return request;
}

export function approveStep(
  stepId: number,
  actorId: number,
  actorName: string,
  comment?: string,
): { success: boolean; notFound?: boolean; alreadyProcessed?: boolean } {
  for (const request of requestsDb) {
    const stepIndex = request.approvalSteps.findIndex((s) => s.id === stepId);
    if (stepIndex === -1) continue;

    const step = request.approvalSteps[stepIndex];
    if (step.status !== "PENDING") return { success: false, alreadyProcessed: true };
    if (request.approvalSteps.slice(0, stepIndex).some((s) => s.status !== "APPROVED")) {
      return { success: false, alreadyProcessed: true };
    }

    const now = new Date().toISOString();
    step.status = "APPROVED";
    step.updatedAt = now;
    request.approvalActions.push({
      id: approvalActionSequence++,
      actorId,
      actorName,
      actionType: "APPROVE",
      comment: comment ?? null,
      createdAt: now,
    });

    if (request.approvalSteps.every((s) => s.status === "APPROVED")) {
      request.status = "APPROVED";
      request.currentStage = "APPROVED";
    } else {
      request.currentStage = "WAITING_MANAGER";
    }
    request.updatedAt = now;
    return { success: true };
  }
  return { success: false, notFound: true };
}

export function rejectStep(
  stepId: number,
  actorId: number,
  actorName: string,
  comment: string,
): { success: boolean; notFound?: boolean; alreadyProcessed?: boolean } {
  for (const request of requestsDb) {
    const step = request.approvalSteps.find((s) => s.id === stepId);
    if (!step) continue;
    if (step.status !== "PENDING") return { success: false, alreadyProcessed: true };

    const now = new Date().toISOString();
    step.status = "REJECTED";
    step.updatedAt = now;
    request.status = "REJECTED";
    request.currentStage = "REJECTED";
    request.updatedAt = now;
    request.approvalActions.push({
      id: approvalActionSequence++,
      actorId,
      actorName,
      actionType: "REJECT",
      comment,
      createdAt: now,
    });
    request.approvalSteps.forEach((s) => {
      if (s.id !== stepId && s.status === "PENDING") {
        s.status = "SKIPPED";
        s.updatedAt = now;
      }
    });
    return { success: true };
  }
  return { success: false, notFound: true };
}

export function listPendingStepsByApprover(approverId: number) {
  return requestsDb.flatMap((request) =>
    request.approvalSteps
      .filter((step, index) =>
        step.approverId === approverId &&
        step.status === "PENDING" &&
        request.approvalSteps.slice(0, index).every((previous) => previous.status === "APPROVED"))
      .map((step) => ({ step, request }))
  );
}

export function computeUsedMinutesByType(
  applicantId: number,
  leaveType: string,
): number {
  return requestsDb
    .filter(
      (r) =>
        r.applicantId === applicantId &&
        r.type === leaveType &&
        (r.status === "PENDING" || r.status === "APPROVED"),
    )
    .reduce((sum, r) => sum + r.durationMinutes, 0);
}

export function toLeaveRequestSummary(request: MockLeaveRequestRecord): LeaveRequestSummary {
  return {
    id: request.id,
    type: request.type,
    startTime: request.startTime,
    endTime: request.endTime,
    durationMinutes: request.durationMinutes,
    status: request.status,
    currentStage: request.currentStage,
    createdAt: request.createdAt,
  };
}

function calculateDurationMinutes(startTime: string, endTime: string): number {
  const start = new Date(startTime);
  const end = new Date(endTime);
  return Math.max(30, Math.round((end.getTime() - start.getTime()) / 60000));
}
