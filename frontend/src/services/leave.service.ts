import ApiService from "@/lib/api-service";
import type {
  CreateLeaveRequest,
  LeaveBalanceResponse,
  LeaveCalculationResponse,
  LeaveRequestDetail,
  LeaveRequestSummary,
  PendingRequestCountResponse,
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
};

export default LeaveService;
