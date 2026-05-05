import ApiService from "@/lib/api-service";
import type {
  AdminLeaveBalance,
  AdminLeaveBalanceFilters,
  AdjustLeaveBalanceRequest,
  AiReviewResponse,
  CreateLeaveRequest,
  InitYearBalancesRequest,
  LeaveBalanceResponse,
  LeaveCalculationResponse,
  LeaveRecallRequest,
  LeaveRequestDetail,
  LeaveRequestSummary,
  PendingRequestCountResponse,
  YearInitResult,
} from "@/types/leave";
import type { PageResponse } from "@/types/common";

const LeaveService = {
  getList(
    page: number,
    size: number
  ): Promise<PageResponse<LeaveRequestSummary>> {
    return ApiService.fetchData({
      url: "/requests",
      method: "GET",
      params: { page, size },
    });
  },

  create(data: CreateLeaveRequest): Promise<LeaveRequestDetail> {
    return ApiService.fetchData<LeaveRequestDetail, CreateLeaveRequest>({
      url: "/requests",
      method: "POST",
      data,
    });
  },

  calculate(startTime: string, endTime: string): Promise<LeaveCalculationResponse> {
    return ApiService.fetchData<LeaveCalculationResponse, { startTime: string; endTime: string }>({
      url: "/requests/calculate",
      method: "POST",
      data: { startTime, endTime },
    });
  },

  getDetail(id: number): Promise<LeaveRequestDetail> {
    return ApiService.fetchData({
      url: `/requests/${id}`,
      method: "GET",
    });
  },

  getAiReview(id: number): Promise<AiReviewResponse> {
    return ApiService.fetchData({
      url: `/requests/${id}/ai-review`,
      method: "GET",
    });
  },

  cancel(id: number): Promise<void> {
    return ApiService.fetchData({
      url: `/requests/${id}/cancel`,
      method: "PATCH",
    });
  },

  getPendingCount(): Promise<PendingRequestCountResponse> {
    return ApiService.fetchData({
      url: "/requests/pending/count",
      method: "GET",
    });
  },

  getBalances(year?: number): Promise<LeaveBalanceResponse[]> {
    return ApiService.fetchData({
      url: "/requests/balance",
      method: "GET",
      params: year ? { year } : undefined,
    });
  },

  recall(id: number, data: LeaveRecallRequest): Promise<void> {
    return ApiService.fetchData({
      url: `/requests/${id}/recall`,
      method: "PATCH",
      data,
    });
  },

  getAdminBalances(
    filters: AdminLeaveBalanceFilters
  ): Promise<PageResponse<AdminLeaveBalance>> {
    return ApiService.fetchData({
      url: "/admin/leave-balances",
      method: "GET",
      params: filters,
    });
  },

  adjustBalance(id: number, data: AdjustLeaveBalanceRequest): Promise<void> {
    return ApiService.fetchData({
      url: `/admin/leave-balances/${id}`,
      method: "PATCH",
      data,
    });
  },

  initYearBalances(data: InitYearBalancesRequest): Promise<YearInitResult> {
    return ApiService.fetchData<YearInitResult, InitYearBalancesRequest>({
      url: "/admin/leave-balances/year-init",
      method: "POST",
      data,
    });
  },
};

export default LeaveService;
