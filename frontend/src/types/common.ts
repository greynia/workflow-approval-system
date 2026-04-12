export interface ApiErrorResponse {
  code: string;
  message: string;
  traceId: string;
}

export interface PaginationFormat {
  currentPage: number;
  totalCount: number;
  pageSize: number;
  totalPages: number;
}
