export type LeaveType = "ANNUAL" | "SICK" | "PERSONAL" | "OTHER";
export type AiReviewStatus = "PENDING" | "COMPLETED" | "FAILED" | "SKIPPED";
export type RiskLevel = "LOW" | "MEDIUM" | "HIGH";
export type AiRecommendation = "APPROVE" | "REVIEW_CAREFULLY" | "ESCALATE" | "INSUFFICIENT_INFORMATION";
export type AiProvider = "RULE_ENGINE" | "LOCAL" | "GEMINI";

export interface HardRuleFlag {
  code: string;
  level: RiskLevel;
  humanReadable: string;
}

export interface AiReviewResponse {
  status: AiReviewStatus;
  summary: string | null;
  riskLevel: RiskLevel | null;
  riskReasons: string[];
  hardRuleFlags: HardRuleFlag[];
  recommendation: AiRecommendation | null;
  recommendationReason: string | null;
  modelName: string | null;
  promptVersion: string | null;
  provider: AiProvider | null;
  errorCode: string | null;
  createdAt: string;
}
export type RequestStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";
export type StepStatus = "PENDING" | "APPROVED" | "REJECTED" | "SKIPPED";
export type ActionType = "APPROVE" | "REJECT";
export type ApprovalStepType = "DEPUTY" | "MANAGER";
export type LeaveRequestStage =
  | "WAITING_DEPUTY"
  | "WAITING_MANAGER"
  | "APPROVED"
  | "REJECTED"
  | "CANCELLED";

export interface ApprovalStepResponse {
  id: number;
  approverId: number;
  approverName: string;
  stepType: ApprovalStepType;
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
  startTime: string;
  endTime: string;
  durationMinutes: number;
  status: RequestStatus;
  currentStage: LeaveRequestStage;
  createdAt: string;
}

export interface LeaveRequestDetail {
  id: number;
  applicantId: number;
  applicantName: string;
  deputyId: number | null;
  deputyName: string | null;
  type: LeaveType;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  reason: string | null;
  status: RequestStatus;
  currentStage: LeaveRequestStage;
  createdAt: string;
  updatedAt: string;
  approvalSteps: ApprovalStepResponse[];
  approvalActions: ApprovalActionResponse[];
}

export interface CreateLeaveRequest {
  type: LeaveType;
  startTime: string;
  endTime: string;
  reason?: string;
  deputyId: number;
}

export interface LeaveCalculationResponse {
  durationMinutes: number;
}

export interface PendingRequestCountResponse {
  count: number;
}

export interface LeaveBalanceResponse {
  leaveType: LeaveType;
  quotaMinutes: number;
  usedMinutes: number;
  remainingMinutes: number;
}
