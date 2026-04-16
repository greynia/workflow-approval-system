import ApiService from "@/lib/api-service";
import type {
  ApproveStepRequest,
  PendingApproval,
  PendingApprovalCountResponse,
  RejectStepRequest,
} from "@/types/approval";

const ApprovalService = {
  getPendingList(): Promise<PendingApproval[]> {
    return ApiService.fetchData({
      url: "/approvals/pending",
      method: "GET",
    });
  },

  getPendingCount(): Promise<PendingApprovalCountResponse> {
    return ApiService.fetchData({
      url: "/approvals/pending/count",
      method: "GET",
    });
  },

  approve(stepId: number, data: ApproveStepRequest): Promise<void> {
    return ApiService.fetchData({
      url: `/approvals/${stepId}/approve`,
      method: "POST",
      data,
    });
  },

  reject(stepId: number, data: RejectStepRequest): Promise<void> {
    return ApiService.fetchData({
      url: `/approvals/${stepId}/reject`,
      method: "POST",
      data,
    });
  },
};

export default ApprovalService;
