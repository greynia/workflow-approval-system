import type { ApprovalStepType, LeaveType, StepStatus } from "./leave";

export interface PendingApproval {
  stepId: number;
  stepType: ApprovalStepType;
  requestId: number;
  applicantId: number;
  applicantName: string;
  leaveType: LeaveType;
  durationMinutes: number;
  startTime: string;
  endTime: string;
  status: StepStatus;
  createdAt: string;
}

export interface PendingApprovalCountResponse {
  count: number;
}

export interface ApproveStepRequest {
  comment?: string;
}

export interface RejectStepRequest {
  comment: string;
}
