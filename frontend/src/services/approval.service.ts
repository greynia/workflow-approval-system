import ApiService from "@/lib/api-service";
import type {
  PendingApproval,
  PendingApprovalCountResponse,
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
};

export default ApprovalService;
