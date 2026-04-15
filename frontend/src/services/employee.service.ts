import ApiService from "@/lib/api-service";
import type { EmployeeSummary } from "@/types/common";

const EmployeeService = {
  getList(): Promise<EmployeeSummary[]> {
    return ApiService.fetchData({
      url: "/employees",
      method: "GET",
    });
  },

  getAvailableDeputies(startTime: string, endTime: string): Promise<EmployeeSummary[]> {
    return ApiService.fetchData({
      url: "/employees/available-deputies",
      method: "GET",
      params: { startTime, endTime },
    });
  },
};

export default EmployeeService;
