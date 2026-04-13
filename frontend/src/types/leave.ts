export type LeaveType = "ANNUAL" | "SICK" | "PERSONAL" | "OTHER";
export type RequestStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";
export type StepStatus = "PENDING" | "APPROVED" | "REJECTED" | "SKIPPED";
export type ActionType = "APPROVE" | "REJECT";

export interface ApprovalStepResponse {
  id: number;
  stepOrder: number;
  approverId: number;
  approverName: string;
  status: StepStatus;
  createdAt: string;
  updatedAt: string;
}

export interface ApprovalActionResponse {
  id: number;
  actorId: number;
  actorName: string;
  actionType: ActionType;
  comment: string | null;
  createdAt: string;
}

export interface LeaveRequestSummary {
  id: number;
  type: LeaveType;
  startDate: string;
  endDate: string;
  days: number;
  status: RequestStatus;
  createdAt: string;
}

export interface LeaveRequestDetail {
  id: number;
  applicantId: number;
  applicantName: string;
  deputyId: number | null;
  deputyName: string | null;
  type: LeaveType;
  startDate: string;
  endDate: string;
  days: number;
  reason: string | null;
  status: RequestStatus;
  createdAt: string;
  updatedAt: string;
  approvalSteps: ApprovalStepResponse[];
  approvalActions: ApprovalActionResponse[];
}

export interface CreateLeaveRequest {
  type: LeaveType;
  startDate: string;
  endDate: string;
  days: number;
  reason?: string;
  deputyId?: number | null;
}
