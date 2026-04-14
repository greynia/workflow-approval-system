import type {
  ApprovalActionResponse,
  ApprovalStepResponse,
  CreateLeaveRequest,
  LeaveRequestDetail,
  LeaveRequestSummary,
  RequestStatus,
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
    startDate: "2026-04-15",
    endDate: "2026-04-16",
    days: 2,
    reason: "Family trip",
    status: "PENDING",
    createdAt: "2026-04-10T09:00:00.000Z",
    updatedAt: "2026-04-10T09:00:00.000Z",
    approvalSteps: [
      {
        id: 5001,
        stepOrder: 1,
        approverId: 2,
        approverName: "Mina Manager",
        status: "PENDING",
        createdAt: "2026-04-10T09:00:00.000Z",
        updatedAt: "2026-04-10T09:00:00.000Z",
      },
    ],
    approvalActions: [],
  },
  {
    id: 1002,
    applicantId: 3,
    applicantName: "Alice Chen",
    deputyId: null,
    deputyName: null,
    type: "SICK",
    startDate: "2026-03-28",
    endDate: "2026-03-28",
    days: 1,
    reason: "Clinic visit",
    status: "APPROVED",
    createdAt: "2026-03-27T08:30:00.000Z",
    updatedAt: "2026-03-27T12:00:00.000Z",
    approvalSteps: [
      {
        id: 5002,
        stepOrder: 1,
        approverId: 2,
        approverName: "Mina Manager",
        status: "APPROVED",
        createdAt: "2026-03-27T08:30:00.000Z",
        updatedAt: "2026-03-27T12:00:00.000Z",
      },
    ],
    approvalActions: [
      {
        id: 7001,
        actorId: 2,
        actorName: "Mina Manager",
        actionType: "APPROVE",
        comment: "Take care",
        createdAt: "2026-03-27T12:00:00.000Z",
      },
    ],
  },
];

let requestsDb: MockLeaveRequestRecord[] = structuredClone(initialMockLeaveRequests);
let requestSequence = 2000;
let approvalStepSequence = 6000;

export function listRequestsByApplicant(applicantId: number): MockLeaveRequestRecord[] {
  return requestsDb
    .filter((request) => request.applicantId === applicantId)
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt));
}

export function findRequestById(id: number): MockLeaveRequestRecord | undefined {
  return requestsDb.find((request) => request.id === id);
}

export function createMockLeaveRequest(input: {
  applicantId: number;
  applicantName: string;
  approverId: number;
  approverName: string;
  deputyId: number | null;
  deputyName: string | null;
  data: CreateLeaveRequest;
}): MockLeaveRequestRecord {
  const now = new Date().toISOString();
  const status: RequestStatus = "PENDING";
  const approvalSteps: ApprovalStepResponse[] = [
    {
      id: approvalStepSequence++,
      stepOrder: 1,
      approverId: input.approverId,
      approverName: input.approverName,
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    },
  ];
  const approvalActions: ApprovalActionResponse[] = [];

  const request: MockLeaveRequestRecord = {
    id: requestSequence++,
    applicantId: input.applicantId,
    applicantName: input.applicantName,
    deputyId: input.deputyId,
    deputyName: input.deputyName,
    type: input.data.type,
    startDate: input.data.startDate,
    endDate: input.data.endDate,
    days: input.data.days,
    reason: input.data.reason ?? null,
    status,
    createdAt: now,
    updatedAt: now,
    approvalSteps,
    approvalActions,
  };

  requestsDb = [request, ...requestsDb];
  return request;
}

export function listPendingStepsByApprover(approverId: number) {
  return requestsDb.flatMap((request) =>
    request.approvalSteps
      .filter((step) => step.approverId === approverId && step.status === "PENDING")
      .map((step) => ({ step, request }))
  );
}

export function toLeaveRequestSummary(
  request: MockLeaveRequestRecord
): LeaveRequestSummary {
  return {
    id: request.id,
    type: request.type,
    startDate: request.startDate,
    endDate: request.endDate,
    days: request.days,
    status: request.status,
    createdAt: request.createdAt,
  };
}
