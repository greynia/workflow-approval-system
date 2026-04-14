import type { LeaveType, StepStatus } from "./leave";

export interface PendingApproval {
  stepId: number;
  stepOrder: number;
  requestId: number;
  applicantId: number;
  applicantName: string;
  leaveType: LeaveType;
  days: number;
  startDate: string;
  endDate: string;
  status: StepStatus;
  createdAt: string;
}

export interface PendingApprovalCountResponse {
  count: number;
}
