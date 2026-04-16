import ApiService from "@/lib/api-service";
import type { EmployeeSummary } from "@/types/common";

const EmployeeService = {
  getList(query?: string): Promise<EmployeeSummary[]> {
    return ApiService.fetchData({
      url: "/employees",
      method: "GET",
      params: {
        ...(query ? { query } : {}),
      },
    });
  },

  getAvailableDeputies(startTime: string, endTime: string, query?: string): Promise<EmployeeSummary[]> {
    return ApiService.fetchData({
      url: "/employees/available-deputies",
      method: "GET",
      params: {
        startTime,
        endTime,
        ...(query ? { query } : {}),
      },
    });
  },
};

export default EmployeeService;
