import type { LeaveType } from "./leave";

export interface PendingRecall {
  stepId: number;
  requestId: number;
  applicantId: number;
  applicantName: string;
  leaveType: LeaveType;
  durationMinutes: number;
  startTime: string;
  endTime: string;
  recallReason: string;
}

export interface RecallDecisionRequest {
  comment?: string;
}

export interface PendingRecallCountResponse {
  count: number;
}
