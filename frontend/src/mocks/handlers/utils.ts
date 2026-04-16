import type { ApiErrorResponse } from "@/types/common";

export function mockErrorBody(code: string, message: string): ApiErrorResponse {
  return {
    code,
    message,
    traceId: `mock-${code.toLowerCase()}`,
  };
}
