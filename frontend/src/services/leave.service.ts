import ApiService from "@/lib/api-service";
import type {
  CreateLeaveRequest,
  LeaveRequestDetail,
  LeaveRequestSummary,
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

  getDetail(id: number): Promise<LeaveRequestDetail> {
    return ApiService.fetchData({
      url: `/requests/${id}`,
      method: "GET",
    });
  },
};

export default LeaveService;
