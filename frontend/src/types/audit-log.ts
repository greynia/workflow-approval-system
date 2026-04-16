export interface AuditLog {
  id: number;
  entityType: string;
  entityId: number;
  action: string;
  actorId: number;
  actorName: string;
  detailJson: string;
  createdAt: string;
}

export interface AuditLogFilters {
  entityType?: string;
  action?: string;
  actorName?: string;
  createdFrom?: string;
  createdTo?: string;
  page: number;
  size: number;
}
