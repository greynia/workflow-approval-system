import ApiService from "@/lib/api-service";
import type {
  PendingRecall,
  PendingRecallCountResponse,
  RecallDecisionRequest,
} from "@/types/recall";

const RecallService = {
  getPendingList(): Promise<PendingRecall[]> {
    return ApiService.fetchData({
      url: "/recalls/pending",
      method: "GET",
    });
  },

  getPendingCount(): Promise<PendingRecallCountResponse> {
    return ApiService.fetchData({
      url: "/recalls/pending/count",
      method: "GET",
    });
  },

  approve(stepId: number): Promise<void> {
    return ApiService.fetchData({
      url: `/recalls/${stepId}/approve`,
      method: "POST",
    });
  },

  reject(stepId: number, data: RecallDecisionRequest): Promise<void> {
    return ApiService.fetchData({
      url: `/recalls/${stepId}/reject`,
      method: "POST",
      data,
    });
  },
};

export default RecallService;
