export interface ApiErrorResponse {
  code: string;
  message: string;
  requestId: string;
}

export interface PageResponse<T> {
  items: T[];
  currentPage: number;
  totalCount: number;
  pageSize: number;
  totalPages: number;
}

export interface EmployeeSummary {
  id: number;
  employeeNo: string;
  name: string;
}
