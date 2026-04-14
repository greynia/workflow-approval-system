import { approvalHandlers } from "./approvals";
import { authHandlers } from "./auth";
import { employeeHandlers } from "./employees";
import { requestHandlers } from "./requests";

export const handlers = [
  ...authHandlers,
  ...employeeHandlers,
  ...requestHandlers,
  ...approvalHandlers,
];
