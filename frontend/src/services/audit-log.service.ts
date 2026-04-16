import ApiService from "@/lib/api-service";
import type { AuditLog, AuditLogFilters } from "@/types/audit-log";
import type { PageResponse } from "@/types/common";

const AuditLogService = {
  getAuditLogs(filters: AuditLogFilters): Promise<PageResponse<AuditLog>> {
    return ApiService.fetchData({
      url: "/admin/audit-logs",
      method: "GET",
      params: {
        ...(filters.entityType && { entityType: filters.entityType }),
        ...(filters.action && { action: filters.action }),
        ...(filters.actorName && { actorName: filters.actorName }),
        ...(filters.createdFrom && { createdFrom: filters.createdFrom }),
        ...(filters.createdTo && { createdTo: filters.createdTo }),
        page: filters.page,
        size: filters.size,
      },
    });
  },
};

export default AuditLogService;
