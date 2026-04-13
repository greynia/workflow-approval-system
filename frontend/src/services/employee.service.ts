import ApiService from "@/lib/api-service";
import type { EmployeeSummary } from "@/types/common";

const EmployeeService = {
  getList(): Promise<EmployeeSummary[]> {
    return ApiService.fetchData({
      url: "/employees",
      method: "GET",
    });
  },
};

export default EmployeeService;
